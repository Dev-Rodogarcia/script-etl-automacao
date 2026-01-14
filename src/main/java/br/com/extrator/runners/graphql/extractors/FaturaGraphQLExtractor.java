package br.com.extrator.runners.graphql.extractors;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.extrator.api.ClienteApiGraphQL;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.FaturaGraphQLEntity;
import br.com.extrator.db.repository.FaturaGraphQLRepository;
import br.com.extrator.db.repository.FaturaPorClienteRepository;
import br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO;
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
        
        final List<FaturaGraphQLEntity> entities = new ArrayList<>();
        
        for (final CreditCustomerBillingNodeDTO dto : dtos) {
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
            
            // Valores
            entity.setValue(dto.getValue());
            entity.setPaidValue(dto.getPaidValue());
            entity.setValueToPay(dto.getValueToPay());
            entity.setDiscountValue(dto.getDiscountValue());
            entity.setInterestValue(dto.getInterestValue());
            entity.setPaid(dto.getPaid());
            
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
            
            // Metadata (JSON completo)
            try {
                entity.setMetadata(objectMapper.writeValueAsString(dto));
            } catch (final JsonProcessingException ex) {
                entity.setMetadata(null);
            }
            
            entities.add(entity);
        }
        
        final int salvos = repository.salvar(entities);
        log.info("✓ Capa Faturas GraphQL salvos: {}/{}", salvos, entities.size());
        
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
    
    @Override
    public String getEntityName() {
        return ConstantesEntidades.FATURAS_GRAPHQL;
    }
    
    @Override
    public String getEmoji() {
        return ConstantesExtracao.EMOJI_FATURAS;
    }
}
