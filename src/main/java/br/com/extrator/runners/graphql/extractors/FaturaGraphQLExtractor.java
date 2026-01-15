package br.com.extrator.runners.graphql.extractors;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.extrator.api.ClienteApiGraphQL;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.FaturaGraphQLEntity;
import br.com.extrator.db.repository.FaturaGraphQLRepository;
import br.com.extrator.db.repository.FaturaPorClienteRepository;
import br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO;
import br.com.extrator.modelo.graphql.bancos.BankAccountNodeDTO;
import br.com.extrator.runners.common.ConstantesExtracao;
import br.com.extrator.runners.common.EntityExtractor;
import br.com.extrator.util.console.LoggerConsole;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Extractor para entidade Faturas GraphQL.
 * Possui lógica especial de mapeamento e enriquecimento.
 */
public class FaturaGraphQLExtractor implements EntityExtractor<CreditCustomerBillingNodeDTO> {
    
    private final ClienteApiGraphQL apiClient;
    private final FaturaGraphQLRepository repository;
    private final FaturaPorClienteRepository faturasPorClienteRepository;
    private final LoggerConsole log;
    private final ObjectMapper objectMapper;
    
    public FaturaGraphQLExtractor(final ClienteApiGraphQL apiClient,
                                 final FaturaGraphQLRepository repository,
                                 final FaturaPorClienteRepository faturasPorClienteRepository,
                                 final LoggerConsole log) {
        this(apiClient, repository, faturasPorClienteRepository, log, new ObjectMapper());
    }
    
    public FaturaGraphQLExtractor(final ClienteApiGraphQL apiClient,
                                 final FaturaGraphQLRepository repository,
                                 final FaturaPorClienteRepository faturasPorClienteRepository,
                                 final LoggerConsole log,
                                 final ObjectMapper objectMapper) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.faturasPorClienteRepository = faturasPorClienteRepository;
        this.log = log;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public ResultadoExtracao<CreditCustomerBillingNodeDTO> extract(final LocalDate dataInicio, final LocalDate dataFim) {
        return apiClient.buscarCapaFaturas(dataInicio, dataFim);
    }
    
    @Override
    public int save(final List<CreditCustomerBillingNodeDTO> dtos) throws java.sql.SQLException {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }
        
        log.info("🔄 Iniciando processamento de {} faturas GraphQL com lógica híbrida", dtos.size());
        
        // PASSO 1: DEDUPLICAÇÃO (Resolve o problema das linhas repetidas)
        final Map<Long, FaturaGraphQLEntity> faturasUnicas = new HashMap<>();
        for (final CreditCustomerBillingNodeDTO dto : dtos) {
            if (dto.getId() == null) {
                log.warn("⚠️ Fatura GraphQL com ID nulo ignorada: {}", dto.getDocument());
                continue;
            }
            
            // Usa o ID da Fatura como chave para garantir unicidade
            if (!faturasUnicas.containsKey(dto.getId())) {
                final FaturaGraphQLEntity entity = mapearDtoParaEntity(dto);
                faturasUnicas.put(dto.getId(), entity);
            }
        }
        
        log.info("✓ Deduplicação concluída: {} faturas únicas de {} totais", faturasUnicas.size(), dtos.size());
        
        // PASSO 2: PREPARAÇÃO DO CACHE BANCÁRIO (Side-Loading)
        final Set<Integer> idsBancos = new HashSet<>();
        final Map<Long, Integer> faturaIdParaBancoId = new HashMap<>();
        
        // PASSO 3: LOOP DE ENRIQUECIMENTO (GraphQL)
        // Primeiro, tenta usar dados já disponíveis na query principal
        for (final CreditCustomerBillingNodeDTO dto : dtos) {
            if (dto.getId() == null) {
                continue;
            }
            
            final FaturaGraphQLEntity entity = faturasUnicas.get(dto.getId());
            if (entity == null) {
                continue;
            }
            
            // Coleta ticketAccountId se já estiver disponível na query principal
            if (dto.getTicketAccountId() != null) {
                idsBancos.add(dto.getTicketAccountId());
                faturaIdParaBancoId.put(dto.getId(), dto.getTicketAccountId());
            }
            
            // Método de pagamento já pode estar na primeira parcela
            if (dto.getInstallments() != null && !dto.getInstallments().isEmpty()) {
                final var parcela = dto.getInstallments().get(0);
                if (parcela.getPaymentMethod() != null && !parcela.getPaymentMethod().trim().isEmpty() 
                        && (entity.getMetodoPagamento() == null || entity.getMetodoPagamento().trim().isEmpty())) {
                    entity.setMetodoPagamento(parcela.getPaymentMethod().trim());
                }
            }
        }
        
        // Agora enriquece apenas as faturas que precisam (falta NFS-e ou ticketAccountId)
        int totalEnriquecidas = 0;
        int totalComErro = 0;
        final Set<Long> faturasParaEnriquecer = new HashSet<>();
        
        for (final Map.Entry<Long, FaturaGraphQLEntity> entry : faturasUnicas.entrySet()) {
            final Long faturaId = entry.getKey();
            final FaturaGraphQLEntity entity = entry.getValue();
            
            // Verifica se precisa enriquecer (falta NFS-e ou ticketAccountId)
            final boolean precisaNfse = entity.getNfseNumero() == null || entity.getNfseNumero().trim().isEmpty();
            final boolean precisaBancoId = !faturaIdParaBancoId.containsKey(faturaId);
            
            if (precisaNfse || precisaBancoId) {
                faturasParaEnriquecer.add(faturaId);
            }
        }
        
        log.info("🔍 {} faturas precisam de enriquecimento adicional (falta NFS-e ou ticketAccountId)", faturasParaEnriquecer.size());
        
        for (final Long faturaId : faturasParaEnriquecer) {
            final FaturaGraphQLEntity entity = faturasUnicas.get(faturaId);
            if (entity == null) {
                continue;
            }
            
            try {
                // CHAMADA GRAPHQL: Dados da Cobrança e NFS-e (apenas se necessário)
                final var dadosCobrancaOpt = apiClient.buscarDadosCobranca(faturaId);
                
                if (dadosCobrancaOpt.isPresent()) {
                    final var dadosCobranca = dadosCobrancaOpt.get();
                    
                    // 1. Pega ID do Banco para o Cache (se ainda não tiver)
                    if (dadosCobranca.getTicketAccountId() != null && !faturaIdParaBancoId.containsKey(faturaId)) {
                        idsBancos.add(dadosCobranca.getTicketAccountId());
                        faturaIdParaBancoId.put(faturaId, dadosCobranca.getTicketAccountId());
                    }
                    
                    // 2. Entra na Parcela para pegar NFS-e e Método Pagamento (se ainda não tiver)
                    if (dadosCobranca.getInstallments() != null && !dadosCobranca.getInstallments().isEmpty()) {
                        final var parcela = dadosCobranca.getInstallments().get(0);
                        
                        // Método de Pagamento (apenas se ainda não tiver)
                        if (parcela.getPaymentMethod() != null && !parcela.getPaymentMethod().trim().isEmpty() 
                                && (entity.getMetodoPagamento() == null || entity.getMetodoPagamento().trim().isEmpty())) {
                            entity.setMetodoPagamento(parcela.getPaymentMethod().trim());
                        }
                        
                        // N° NFS-e (apenas se ainda não tiver)
                        if (parcela.getAccountingCredit() != null) {
                            final String nfseNumero = parcela.getAccountingCredit().getDocument();
                            if (nfseNumero != null && !nfseNumero.trim().isEmpty() 
                                    && (entity.getNfseNumero() == null || entity.getNfseNumero().trim().isEmpty())) {
                                entity.setNfseNumero(nfseNumero.trim());
                            }
                        }
                    }
                    
                    totalEnriquecidas++;
                }
            } catch (final Exception e) {
                log.warn("⚠️ Erro ao enriquecer fatura ID {}: {}", faturaId, e.getMessage());
                totalComErro++;
            }
        }
        
        log.info("✓ Enriquecimento concluído: {} faturas enriquecidas, {} erros, {} IDs de bancos únicos coletados", 
                totalEnriquecidas, totalComErro, idsBancos.size());
        
        // PASSO 4: CACHE BANCÁRIO (Busca detalhes apenas uma vez por Banco)
        final Map<Integer, BankAccountNodeDTO> cacheBanco = new HashMap<>();
        int totalBancosBuscados = 0;
        
        for (final Integer idBanco : idsBancos) {
            try {
                // CHAMADA GRAPHQL 2: Detalhes do Banco
                final var dadosBancoOpt = apiClient.buscarDetalhesBanco(idBanco);
                
                if (dadosBancoOpt.isPresent()) {
                    cacheBanco.put(idBanco, dadosBancoOpt.get());
                    totalBancosBuscados++;
                }
            } catch (final Exception e) {
                log.warn("⚠️ Erro ao buscar detalhes do banco ID {}: {}", idBanco, e.getMessage());
            }
        }
        
        log.info("✓ Cache bancário preenchido: {} bancos buscados com sucesso", totalBancosBuscados);
        
        // PASSO 5: MERGE FINAL
        for (final Map.Entry<Long, FaturaGraphQLEntity> entry : faturasUnicas.entrySet()) {
            final Long faturaId = entry.getKey();
            final FaturaGraphQLEntity entity = entry.getValue();
            
            final Integer bancoIdTemporario = faturaIdParaBancoId.get(faturaId);
            if (bancoIdTemporario != null) {
                final var infoBanco = cacheBanco.get(bancoIdTemporario);
                if (infoBanco != null) {
                    // Nome do Banco
                    if (infoBanco.getBankName() != null && !infoBanco.getBankName().trim().isEmpty()) {
                        entity.setBancoNome(infoBanco.getBankName().trim());
                    }
                    
                    // Carteira (pode vir vazio se não cadastrado)
                    if (infoBanco.getPortfolioVariation() != null && !infoBanco.getPortfolioVariation().trim().isEmpty()) {
                        entity.setCarteiraBanco(infoBanco.getPortfolioVariation().trim());
                    }
                    
                    // Instrução Customizada (pode vir vazio se não cadastrado)
                    if (infoBanco.getCustomInstruction() != null && !infoBanco.getCustomInstruction().trim().isEmpty()) {
                        entity.setInstrucaoBoleto(infoBanco.getCustomInstruction().trim());
                    }
                }
            }
        }
        
        // PASSO 6: SALVAR NO SQL (Upsert)
        final List<FaturaGraphQLEntity> entitiesParaSalvar = new ArrayList<>(faturasUnicas.values());
        final int salvos = repository.salvar(entitiesParaSalvar);
        log.info("✓ Capa Faturas GraphQL salvos: {}/{}", salvos, entitiesParaSalvar.size());
        
        // Enriquecimento via tabela ponte
        try {
            final int nfseAtualizadas = faturasPorClienteRepository.enriquecerNumeroNfseViaTabelaPonte();
            log.info("✓ Relatório Faturas enriquecido com NFS-e: {} linhas atualizadas", nfseAtualizadas);
            final int pagadorAtualizadas = faturasPorClienteRepository.enriquecerPagadorViaTabelaPonte();
            log.info("✓ Relatório Faturas enriquecido com Pagador: {} linhas atualizadas", pagadorAtualizadas);
        } catch (final java.sql.SQLException e) {
            log.warn("⚠️ Enriquecimento via tabela ponte ignorado: {}", e.getMessage());
        }
        
        return salvos;
    }
    
    /**
     * Mapeia um DTO de fatura GraphQL para uma Entity.
     * Extrai todos os campos básicos (sem enriquecimento via queries adicionais).
     */
    private FaturaGraphQLEntity mapearDtoParaEntity(final CreditCustomerBillingNodeDTO dto) {
        final FaturaGraphQLEntity entity = new FaturaGraphQLEntity();
        entity.setId(dto.getId());
        entity.setDocument(dto.getDocument());
        
        // Datas
        try {
            entity.setIssueDate(dto.getIssueDate() != null ? LocalDate.parse(dto.getIssueDate()) : null);
        } catch (final Exception ignored) {
            // Ignorar erros de parsing
        }
        try {
            entity.setDueDate(dto.getDueDate() != null ? LocalDate.parse(dto.getDueDate()) : null);
        } catch (final Exception ignored) {
            // Ignorar erros de parsing
        }
        try {
            if (dto.getInstallments() != null && !dto.getInstallments().isEmpty()) {
                final String originalDueDate = dto.getInstallments().get(0).getOriginalDueDate();
                entity.setOriginalDueDate(originalDueDate != null ? LocalDate.parse(originalDueDate) : null);
            }
        } catch (final Exception ignored) {
            // Ignorar erros de parsing
        }
        
        // Valores
        entity.setValue(dto.getValue());
        entity.setPaidValue(dto.getPaidValue());
        entity.setValueToPay(dto.getValueToPay());
        entity.setDiscountValue(dto.getDiscountValue());
        entity.setInterestValue(dto.getInterestValue());
        entity.setPaid(dto.getPaid());
        
        // Status
        if (dto.getInstallments() != null && !dto.getInstallments().isEmpty()) {
            entity.setStatus(dto.getInstallments().get(0).getStatus());
        }
        
        // Tipo e comentários
        entity.setType(dto.getType());
        entity.setComments(dto.getComments());
        entity.setSequenceCode(dto.getSequenceCode());
        entity.setCompetenceMonth(dto.getCompetenceMonth());
        entity.setCompetenceYear(dto.getCompetenceYear());
        
        // Corporation
        if (dto.getCorporation() != null) {
            try {
                if (dto.getCorporation().getId() != null) {
                    try {
                        entity.setCorporationId(Long.valueOf(dto.getCorporation().getId()));
                    } catch (final NumberFormatException ex) {
                        entity.setCorporationId(null);
                    }
                }
            } catch (final Exception ignored) {
                // Ignorar erros
            }
            
            if (dto.getCorporation().getPerson() != null) {
                entity.setCorporationName(dto.getCorporation().getPerson().getNickname());
                entity.setCorporationCnpj(dto.getCorporation().getPerson().getCnpj());
            }
        }
        
        // Campos básicos da primeira parcela (podem ser sobrescritos no enriquecimento)
        if (dto.getInstallments() != null && !dto.getInstallments().isEmpty()) {
            final CreditCustomerBillingNodeDTO.InstallmentDTO primeiraParcela = dto.getInstallments().get(0);
            
            // N° NFS-e (pode ser sobrescrito no enriquecimento)
            if (primeiraParcela.getAccountingCredit() != null) {
                final String nfseNumero = primeiraParcela.getAccountingCredit().getDocument();
                if (nfseNumero != null && !nfseNumero.trim().isEmpty()) {
                    entity.setNfseNumero(nfseNumero.trim());
                }
            }
            
            // Carteira e Instrução (podem ser sobrescritos no enriquecimento)
            if (primeiraParcela.getAccountingBankAccount() != null) {
                final CreditCustomerBillingNodeDTO.AccountingBankAccountDTO contaBancaria = 
                    primeiraParcela.getAccountingBankAccount();
                
                final String carteira = contaBancaria.getPortfolioVariation();
                if (carteira != null && !carteira.trim().isEmpty()) {
                    entity.setCarteiraBanco(carteira.trim());
                }
                
                final String instrucao = contaBancaria.getCustomInstruction();
                if (instrucao != null && !instrucao.trim().isEmpty()) {
                    entity.setInstrucaoBoleto(instrucao.trim());
                }
                
                // Nome do banco (pode vir da parcela, mas será sobrescrito se houver enriquecimento)
                final String bancoNome = contaBancaria.getBankName();
                if (bancoNome != null && !bancoNome.trim().isEmpty()) {
                    entity.setBancoNome(bancoNome.trim());
                }
            }
            
            // Método de Pagamento (pode ser sobrescrito no enriquecimento)
            if (primeiraParcela.getPaymentMethod() != null && !primeiraParcela.getPaymentMethod().trim().isEmpty()) {
                entity.setMetodoPagamento(primeiraParcela.getPaymentMethod().trim());
            }
        }
        
        // Metadata (JSON completo)
        try {
            entity.setMetadata(objectMapper.writeValueAsString(dto));
        } catch (final JsonProcessingException ex) {
            entity.setMetadata(null);
        }
        
        return entity;
    }
    
    @Override
    public String getEntityName() {
        return ConstantesEntidades.FATURAS_GRAPHQL;
    }
    
    @Override
    public String getEmoji() {
        return ConstantesExtracao.EMOJI_FATURAS;
    }
}
