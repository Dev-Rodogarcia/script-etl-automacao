package br.com.extrator.runners;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import br.com.extrator.api.ClienteApiGraphQL;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.ColetaEntity;
import br.com.extrator.db.entity.FreteEntity;
import br.com.extrator.db.entity.LogExtracaoEntity;
import br.com.extrator.db.repository.ColetaRepository;
import br.com.extrator.db.repository.FaturaGraphQLRepository;
import br.com.extrator.db.repository.FreteRepository;
import br.com.extrator.db.repository.LogExtracaoRepository;
import br.com.extrator.modelo.graphql.coletas.ColetaMapper;
import br.com.extrator.modelo.graphql.coletas.ColetaNodeDTO;
import br.com.extrator.modelo.graphql.fretes.FreteMapper;
import br.com.extrator.modelo.graphql.fretes.FreteNodeDTO;
import br.com.extrator.util.banco.GerenciadorConexao;
import br.com.extrator.util.configuracao.CarregadorConfig;
import br.com.extrator.util.console.LoggerConsole;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Runner independente para a API GraphQL (Coletas, Fretes e Faturas GraphQL).
 */
public final class GraphQLRunner {

    private static final LoggerConsole log = LoggerConsole.getLogger(GraphQLRunner.class);

    private GraphQLRunner() {}

    /**
     * Executa extração de todas as entidades GraphQL.
     * 
     * @param dataInicio Data de início para filtro
     * @throws Exception Se houver falha na extração
     */
    public static void executar(final LocalDate dataInicio) throws Exception {
        executar(dataInicio, (String) null);
    }

    /**
     * Executa extração de todas as entidades GraphQL para um intervalo de datas.
     * 
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @throws Exception Se houver falha na extração
     */
    public static void executarPorIntervalo(final LocalDate dataInicio, final LocalDate dataFim) throws Exception {
        executarPorIntervalo(dataInicio, dataFim, null);
    }

    /**
     * Executa extração de entidade(s) GraphQL específica(s) para um intervalo de datas.
     * 
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @param entidade Nome da entidade (null = todas)
     * @throws Exception Se houver falha na extração
     */
    public static void executarPorIntervalo(final LocalDate dataInicio, final LocalDate dataFim, final String entidade) throws Exception {
        log.info("🔄 Executando runner GraphQL - Período: {} a {}", dataInicio, dataFim);
        CarregadorConfig.validarConexaoBancoDados();

        final ClienteApiGraphQL clienteApiGraphQL = new ClienteApiGraphQL();
        clienteApiGraphQL.setExecutionUuid(java.util.UUID.randomUUID().toString());
        final ColetaRepository coletaRepository = new ColetaRepository();
        final FreteRepository freteRepository = new FreteRepository();
        final LogExtracaoRepository logExtracaoRepository = new LogExtracaoRepository();

        final ColetaMapper coletaMapper = new ColetaMapper();
        final FreteMapper freteMapper = new FreteMapper();

        logExtracaoRepository.criarTabelaSeNaoExistir();

        final boolean executarColetas = entidade == null || entidade.isBlank() || ConstantesEntidades.COLETAS.equalsIgnoreCase(entidade);
        final boolean executarFretes = entidade == null || entidade.isBlank() || ConstantesEntidades.FRETES.equalsIgnoreCase(entidade);
        final boolean executarFaturasGraphql = entidade == null || entidade.isBlank() || ConstantesEntidades.FATURAS_GRAPHQL.equalsIgnoreCase(entidade) || java.util.Arrays.stream(ConstantesEntidades.ALIASES_FATURAS_GRAPHQL).anyMatch(alias -> alias.equalsIgnoreCase(entidade));

        if (executarColetas) {
            log.info("\n📦 Extraindo Coletas...");
            final LocalDateTime inicioColetas = LocalDateTime.now();
            try {
                final ResultadoExtracao<ColetaNodeDTO> resultadoColetas = clienteApiGraphQL.buscarColetas(dataInicio, dataFim);
                final List<ColetaNodeDTO> coletasDTO = resultadoColetas.getDados();
                final String statusColetas = resultadoColetas.isCompleto() ? "" : " (INCOMPLETO: " + resultadoColetas.getMotivoInterrupcao() + ")";
                log.info("✓ Extraídas: {} coletas{}", coletasDTO.size(), statusColetas);
                int registrosSalvos = 0;
                if (!coletasDTO.isEmpty()) {
                    final List<ColetaEntity> coletasEntities = coletasDTO.stream()
                        .map(coletaMapper::toEntity)
                        .collect(Collectors.toList());
                    registrosSalvos = coletaRepository.salvar(coletasEntities);
                    log.info("✓ Salvas: {}/{} coletas", registrosSalvos, coletasDTO.size());
                }
                final String status = resultadoColetas.isCompleto() ? ConstantesEntidades.STATUS_COMPLETO : ConstantesEntidades.STATUS_INCOMPLETO;
                final int totalRecebido = coletasDTO.size();
                final int deltaIgnorados = Math.max(0, totalRecebido - registrosSalvos);
                final String mensagem = "API: " + totalRecebido + " recebidos | DB: " + registrosSalvos + " processados | Delta: " + deltaIgnorados + " (duplicados/ignorados) | Período: " + dataInicio + " a " + dataFim;
                final LogExtracaoEntity logColetas = new LogExtracaoEntity(
                    ConstantesEntidades.COLETAS,
                    inicioColetas,
                    LocalDateTime.now(),
                    status,
                    registrosSalvos,
                    resultadoColetas.getPaginasProcessadas(),
                    mensagem
                );
                logExtracaoRepository.gravarLogExtracao(logColetas);
            } catch (RuntimeException | java.sql.SQLException e) {
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    ConstantesEntidades.COLETAS,
                    inicioColetas,
                    LocalDateTime.now(),
                    ConstantesEntidades.STATUS_ERRO_API,
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
                throw new RuntimeException("Falha na extração de coletas", e);
            }
        }

        if (executarFretes) {
            log.info("\n🚛 Extraindo Fretes...");
            final LocalDateTime inicioFretes = LocalDateTime.now();
            try {
                final ResultadoExtracao<FreteNodeDTO> resultadoFretes = clienteApiGraphQL.buscarFretes(dataInicio, dataFim);
                final List<FreteNodeDTO> fretesDTO = resultadoFretes.getDados();
                final String statusFretes = resultadoFretes.isCompleto() ? "" : " (INCOMPLETO: " + resultadoFretes.getMotivoInterrupcao() + ")";
                log.info("✓ Extraídos: {} fretes{}", fretesDTO.size(), statusFretes);
                int registrosSalvos = 0;
                if (!fretesDTO.isEmpty()) {
                    final List<FreteEntity> fretesEntities = fretesDTO.stream()
                        .map(freteMapper::toEntity)
                        .collect(Collectors.toList());
                    registrosSalvos = freteRepository.salvar(fretesEntities);
                    log.info("✓ Salvos: {}/{} fretes", registrosSalvos, fretesDTO.size());
                }
                final String status = resultadoFretes.isCompleto() ? ConstantesEntidades.STATUS_COMPLETO : ConstantesEntidades.STATUS_INCOMPLETO;
                final int totalRecebido = fretesDTO.size();
                final int deltaIgnorados = Math.max(0, totalRecebido - registrosSalvos);
                final String mensagem = "API: " + totalRecebido + " recebidos | DB: " + registrosSalvos + " processados | Delta: " + deltaIgnorados + " (duplicados/ignorados) | Período: " + dataInicio + " a " + dataFim;
                final LogExtracaoEntity logFretes = new LogExtracaoEntity(
                    ConstantesEntidades.FRETES,
                    inicioFretes,
                    LocalDateTime.now(),
                    status,
                    registrosSalvos,
                    resultadoFretes.getPaginasProcessadas(),
                    mensagem
                );
                logExtracaoRepository.gravarLogExtracao(logFretes);
            } catch (RuntimeException | java.sql.SQLException e) {
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    ConstantesEntidades.FRETES,
                    inicioFretes,
                    LocalDateTime.now(),
                    ConstantesEntidades.STATUS_ERRO_API,
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
                throw new RuntimeException("Falha na extração de fretes", e);
            }
        }
        
        if (executarFaturasGraphql) {
            log.info("\n🧾 Processando Capa Faturas (GraphQL) - Período: {} a {}...", dataInicio, dataFim);
            try (Connection conn = GerenciadorConexao.obterConexao()) {
                new FaturaGraphQLRepository().criarTabelaSeNaoExistirPublico(conn);
            } catch (final java.sql.SQLException e) {
                log.warn("⚠️ Falha ao verificar/criar tabela faturas_graphql: {}", e.getMessage());
            }
            final LocalDateTime inicioFaturas = LocalDateTime.now();
            try {
                // Para faturas, usar o intervalo completo dataInicio a dataFim
                final ResultadoExtracao<br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO> capaResultado = clienteApiGraphQL.buscarCapaFaturas(dataInicio, dataFim);
                final java.util.List<br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO> capaList = capaResultado.getDados();
                if (!capaList.isEmpty()) {
                    final br.com.extrator.db.repository.FaturaGraphQLRepository repo = new br.com.extrator.db.repository.FaturaGraphQLRepository();
                    final java.util.List<br.com.extrator.db.entity.FaturaGraphQLEntity> entities = new java.util.ArrayList<>();
                    final com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    for (final br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO dto : capaList) {
                        final br.com.extrator.db.entity.FaturaGraphQLEntity e = new br.com.extrator.db.entity.FaturaGraphQLEntity();
                        e.setId(dto.getId());
                        e.setDocument(dto.getDocument());
                        
                        // Datas
                        try { e.setIssueDate(dto.getIssueDate() != null ? java.time.LocalDate.parse(dto.getIssueDate()) : null); } catch (final Exception ignored) {}
                        try { e.setDueDate(dto.getDueDate() != null ? java.time.LocalDate.parse(dto.getDueDate()) : null); } catch (final Exception ignored) {}
                        // originalDueDate não existe em CreditBase, pode ser obtido dos installments se necessário
                        
                        // Valores
                        e.setValue(dto.getValue());
                        e.setPaidValue(dto.getPaidValue());
                        e.setValueToPay(dto.getValueToPay());
                        e.setDiscountValue(dto.getDiscountValue());
                        e.setInterestValue(dto.getInterestValue());
                        e.setPaid(dto.getPaid());
                        
                        // Tipo e comentários
                        e.setType(dto.getType());
                        e.setComments(dto.getComments());
                        e.setSequenceCode(dto.getSequenceCode());
                        e.setCompetenceMonth(dto.getCompetenceMonth());
                        e.setCompetenceYear(dto.getCompetenceYear());
                        
                        // Status, createdAt e updatedAt não existem em CreditBase
                        // Podem ser calculados ou obtidos de outras fontes se necessário
                        
                        // Corporation
                        if (dto.getCorporation() != null) {
                            try {
                                if (dto.getCorporation().getId() != null) {
                                    try { e.setCorporationId(Long.valueOf(dto.getCorporation().getId())); } catch (final NumberFormatException ex) { e.setCorporationId(null); }
                                }
                            } catch (final Exception ignored) {}
                            
                            if (dto.getCorporation().getPerson() != null) {
                                e.setCorporationName(dto.getCorporation().getPerson().getNickname());
                                e.setCorporationCnpj(dto.getCorporation().getPerson().getCnpj());
                            }
                        }
                        
                        // Metadata (JSON completo)
                        try { e.setMetadata(om.writeValueAsString(dto)); } catch (final com.fasterxml.jackson.core.JsonProcessingException ex) { e.setMetadata(null); }
                        entities.add(e);
                    }
                    final int salvos = repo.salvar(entities);
                    log.info("✓ Capa Faturas GraphQL salvos: {}/{}", salvos, entities.size());
                    try {
                        final br.com.extrator.db.repository.FaturaPorClienteRepository fpcRepo = new br.com.extrator.db.repository.FaturaPorClienteRepository();
                        final int nfseAtualizadas = fpcRepo.enriquecerNumeroNfseViaTabelaPonte();
                        log.info("✓ Relatório Faturas enriquecido com NFS-e: {} linhas atualizadas", nfseAtualizadas);
                        final int pagadorAtualizadas = fpcRepo.enriquecerPagadorViaTabelaPonte();
                        log.info("✓ Relatório Faturas enriquecido com Pagador: {} linhas atualizadas", pagadorAtualizadas);
                    } catch (final java.sql.SQLException e) {
                        log.warn("⚠️ Enriquecimento via tabela ponte ignorado: {}", e.getMessage());
                    }
                } else {
                    log.info("ℹ️ Capa Faturas GraphQL retornou 0 registros");
                }
                final String statusFaturas = capaResultado.isCompleto() ? "COMPLETO" : "INCOMPLETO";
                final int totalRecebidoFaturas = capaList.size();
                final int registrosSalvosFaturas = totalRecebidoFaturas; 
                final int deltaIgnoradosFaturas = Math.max(0, totalRecebidoFaturas - registrosSalvosFaturas);
                final String msgFaturas = "API: " + totalRecebidoFaturas + " recebidos | DB: " + registrosSalvosFaturas + " processados | Delta: " + deltaIgnoradosFaturas + " | Período: " + dataInicio + " a " + dataFim;
                final LogExtracaoEntity logFaturas = new LogExtracaoEntity(
                    ConstantesEntidades.FATURAS_GRAPHQL,
                    inicioFaturas,
                    LocalDateTime.now(),
                    statusFaturas,
                    registrosSalvosFaturas,
                    capaResultado.getPaginasProcessadas(),
                    msgFaturas
                );
                logExtracaoRepository.gravarLogExtracao(logFaturas);
            } catch (final java.sql.SQLException | RuntimeException e) {
                log.warn("⚠️ Falha no processamento de Capa Faturas GraphQL: {}", e.getMessage());
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    ConstantesEntidades.FATURAS_GRAPHQL,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    ConstantesEntidades.STATUS_ERRO_API,
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
            }
        }
        logExtracaoRepository.criarOuAtualizarViewDimFiliais();
        logExtracaoRepository.criarOuAtualizarViewDimClientes();
        logExtracaoRepository.criarOuAtualizarViewDimVeiculos();
        logExtracaoRepository.criarOuAtualizarViewDimMotoristas();
    }

    /**
     * Executa extração de entidade(s) GraphQL específica(s).
     * 
     * @param dataInicio Data de início para filtro
     * @param entidade Nome da entidade (null = todas)
     * @throws Exception Se houver falha na extração
     */
    public static void executar(final LocalDate dataInicio, final String entidade) throws Exception {
        log.info("🔄 Executando runner GraphQL...");
        CarregadorConfig.validarConexaoBancoDados();

        final ClienteApiGraphQL clienteApiGraphQL = new ClienteApiGraphQL();
        clienteApiGraphQL.setExecutionUuid(java.util.UUID.randomUUID().toString());
        final ColetaRepository coletaRepository = new ColetaRepository();
        final FreteRepository freteRepository = new FreteRepository();
        final LogExtracaoRepository logExtracaoRepository = new LogExtracaoRepository();

        final ColetaMapper coletaMapper = new ColetaMapper();
        final FreteMapper freteMapper = new FreteMapper();

        logExtracaoRepository.criarTabelaSeNaoExistir();

        final boolean executarColetas = entidade == null || entidade.isBlank() || ConstantesEntidades.COLETAS.equalsIgnoreCase(entidade);
        final boolean executarFretes = entidade == null || entidade.isBlank() || ConstantesEntidades.FRETES.equalsIgnoreCase(entidade);
        final boolean executarFaturasGraphql = entidade == null || entidade.isBlank() || ConstantesEntidades.FATURAS_GRAPHQL.equalsIgnoreCase(entidade) || java.util.Arrays.stream(ConstantesEntidades.ALIASES_FATURAS_GRAPHQL).anyMatch(alias -> alias.equalsIgnoreCase(entidade));

        if (executarColetas) {
            log.info("\n📦 Extraindo Coletas...");
            final LocalDateTime inicioColetas = LocalDateTime.now();
            try {
                final ResultadoExtracao<ColetaNodeDTO> resultadoColetas = clienteApiGraphQL.buscarColetas(dataInicio);
                final List<ColetaNodeDTO> coletasDTO = resultadoColetas.getDados();
                final String statusColetas = resultadoColetas.isCompleto() ? "" : " (INCOMPLETO: " + resultadoColetas.getMotivoInterrupcao() + ")";
                log.info("✓ Extraídas: {} coletas{}", coletasDTO.size(), statusColetas);
                int registrosSalvos = 0;
                if (!coletasDTO.isEmpty()) {
                    final List<ColetaEntity> coletasEntities = coletasDTO.stream()
                        .map(coletaMapper::toEntity)
                        .collect(Collectors.toList());
                    registrosSalvos = coletaRepository.salvar(coletasEntities);
                    log.info("✓ Salvas: {}/{} coletas", registrosSalvos, coletasDTO.size());
                }
                final String status = resultadoColetas.isCompleto() ? ConstantesEntidades.STATUS_COMPLETO : ConstantesEntidades.STATUS_INCOMPLETO;
                final int totalRecebido = coletasDTO.size();
                final int deltaIgnorados = Math.max(0, totalRecebido - registrosSalvos);
                final String mensagem = "API: " + totalRecebido + " recebidos | DB: " + registrosSalvos + " processados | Delta: " + deltaIgnorados + " (duplicados/ignorados)";
                final LogExtracaoEntity logColetas = new LogExtracaoEntity(
                    ConstantesEntidades.COLETAS,
                    inicioColetas,
                    LocalDateTime.now(),
                    status,
                    registrosSalvos,
                    resultadoColetas.getPaginasProcessadas(),
                    mensagem
                );
                logExtracaoRepository.gravarLogExtracao(logColetas);
            } catch (RuntimeException | java.sql.SQLException e) {
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    ConstantesEntidades.COLETAS,
                    inicioColetas,
                    LocalDateTime.now(),
                    ConstantesEntidades.STATUS_ERRO_API,
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
                throw new RuntimeException("Falha na extração de coletas", e);
            }
        }

        if (executarFretes) {
            log.info("\n🚛 Extraindo Fretes...");
            final LocalDateTime inicioFretes = LocalDateTime.now();
            try {
                final ResultadoExtracao<FreteNodeDTO> resultadoFretes = clienteApiGraphQL.buscarFretes(dataInicio);
                final List<FreteNodeDTO> fretesDTO = resultadoFretes.getDados();
                final String statusFretes = resultadoFretes.isCompleto() ? "" : " (INCOMPLETO: " + resultadoFretes.getMotivoInterrupcao() + ")";
                log.info("✓ Extraídos: {} fretes{}", fretesDTO.size(), statusFretes);
                int registrosSalvos = 0;
                if (!fretesDTO.isEmpty()) {
                    final List<FreteEntity> fretesEntities = fretesDTO.stream()
                        .map(freteMapper::toEntity)
                        .collect(Collectors.toList());
                    registrosSalvos = freteRepository.salvar(fretesEntities);
                    log.info("✓ Salvos: {}/{} fretes", registrosSalvos, fretesDTO.size());
                }
                final String status = resultadoFretes.isCompleto() ? ConstantesEntidades.STATUS_COMPLETO : ConstantesEntidades.STATUS_INCOMPLETO;
                final int totalRecebido = fretesDTO.size();
                final int deltaIgnorados = Math.max(0, totalRecebido - registrosSalvos);
                final String mensagem = "API: " + totalRecebido + " recebidos | DB: " + registrosSalvos + " processados | Delta: " + deltaIgnorados + " (duplicados/ignorados)";
                final LogExtracaoEntity logFretes = new LogExtracaoEntity(
                    ConstantesEntidades.FRETES,
                    inicioFretes,
                    LocalDateTime.now(),
                    status,
                    registrosSalvos,
                    resultadoFretes.getPaginasProcessadas(),
                    mensagem
                );
                logExtracaoRepository.gravarLogExtracao(logFretes);
            } catch (RuntimeException | java.sql.SQLException e) {
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    ConstantesEntidades.FRETES,
                    inicioFretes,
                    LocalDateTime.now(),
                    ConstantesEntidades.STATUS_ERRO_API,
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
                throw new RuntimeException("Falha na extração de fretes", e);
            }
        }
        
        if (executarFaturasGraphql) {
            log.info("\n🧾 Processando Capa Faturas (GraphQL)...");
            try (Connection conn = GerenciadorConexao.obterConexao()) {
                new FaturaGraphQLRepository().criarTabelaSeNaoExistirPublico(conn);
            } catch (final java.sql.SQLException e) {
                log.warn("⚠️ Falha ao verificar/criar tabela faturas_graphql: {}", e.getMessage());
            }
            final LocalDateTime inicioFaturas = LocalDateTime.now();
            try {
                final ResultadoExtracao<br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO> capaResultado = clienteApiGraphQL.buscarCapaFaturas(LocalDate.now());
                final java.util.List<br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO> capaList = capaResultado.getDados();
                if (!capaList.isEmpty()) {
                    final br.com.extrator.db.repository.FaturaGraphQLRepository repo = new br.com.extrator.db.repository.FaturaGraphQLRepository();
                    final java.util.List<br.com.extrator.db.entity.FaturaGraphQLEntity> entities = new java.util.ArrayList<>();
                    final com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    for (final br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO dto : capaList) {
                        final br.com.extrator.db.entity.FaturaGraphQLEntity e = new br.com.extrator.db.entity.FaturaGraphQLEntity();
                        e.setId(dto.getId());
                        e.setDocument(dto.getDocument());
                        
                        // Datas
                        try { e.setIssueDate(dto.getIssueDate() != null ? java.time.LocalDate.parse(dto.getIssueDate()) : null); } catch (final Exception ignored) {}
                        try { e.setDueDate(dto.getDueDate() != null ? java.time.LocalDate.parse(dto.getDueDate()) : null); } catch (final Exception ignored) {}
                        // originalDueDate não existe em CreditBase, pode ser obtido dos installments se necessário
                        
                        // Valores
                        e.setValue(dto.getValue());
                        e.setPaidValue(dto.getPaidValue());
                        e.setValueToPay(dto.getValueToPay());
                        e.setDiscountValue(dto.getDiscountValue());
                        e.setInterestValue(dto.getInterestValue());
                        e.setPaid(dto.getPaid());
                        
                        // Tipo e comentários
                        e.setType(dto.getType());
                        e.setComments(dto.getComments());
                        e.setSequenceCode(dto.getSequenceCode());
                        e.setCompetenceMonth(dto.getCompetenceMonth());
                        e.setCompetenceYear(dto.getCompetenceYear());
                        
                        // Status, createdAt e updatedAt não existem em CreditBase
                        // Podem ser calculados ou obtidos de outras fontes se necessário
                        
                        // Corporation
                        if (dto.getCorporation() != null) {
                            try {
                                if (dto.getCorporation().getId() != null) {
                                    try { e.setCorporationId(Long.valueOf(dto.getCorporation().getId())); } catch (final NumberFormatException ex) { e.setCorporationId(null); }
                                }
                            } catch (final Exception ignored) {}
                            
                            if (dto.getCorporation().getPerson() != null) {
                                e.setCorporationName(dto.getCorporation().getPerson().getNickname());
                                e.setCorporationCnpj(dto.getCorporation().getPerson().getCnpj());
                            }
                        }
                        
                        // Metadata (JSON completo)
                        try { e.setMetadata(om.writeValueAsString(dto)); } catch (final com.fasterxml.jackson.core.JsonProcessingException ex) { e.setMetadata(null); }
                        entities.add(e);
                    }
                    final int salvos = repo.salvar(entities);
                    log.info("✓ Capa Faturas GraphQL salvos: {}/{}", salvos, entities.size());
                    try {
                        final br.com.extrator.db.repository.FaturaPorClienteRepository fpcRepo = new br.com.extrator.db.repository.FaturaPorClienteRepository();
                        final int nfseAtualizadas = fpcRepo.enriquecerNumeroNfseViaTabelaPonte();
                        log.info("✓ Relatório Faturas enriquecido com NFS-e: {} linhas atualizadas", nfseAtualizadas);
                        final int pagadorAtualizadas = fpcRepo.enriquecerPagadorViaTabelaPonte();
                        log.info("✓ Relatório Faturas enriquecido com Pagador: {} linhas atualizadas", pagadorAtualizadas);
                    } catch (final java.sql.SQLException e) {
                        log.warn("⚠️ Enriquecimento via tabela ponte ignorado: {}", e.getMessage());
                    }
                } else {
                    log.info("ℹ️ Capa Faturas GraphQL retornou 0 registros");
                }
                final String statusFaturas = capaResultado.isCompleto() ? "COMPLETO" : "INCOMPLETO";
                final int totalRecebidoFaturas = capaList.size();
                final int registrosSalvosFaturas = totalRecebidoFaturas; 
                final int deltaIgnoradosFaturas = Math.max(0, totalRecebidoFaturas - registrosSalvosFaturas);
                final String msgFaturas = "API: " + totalRecebidoFaturas + " recebidos | DB: " + registrosSalvosFaturas + " processados | Delta: " + deltaIgnoradosFaturas;
                final LogExtracaoEntity logFaturas = new LogExtracaoEntity(
                    ConstantesEntidades.FATURAS_GRAPHQL,
                    inicioFaturas,
                    LocalDateTime.now(),
                    statusFaturas,
                    registrosSalvosFaturas,
                    capaResultado.getPaginasProcessadas(),
                    msgFaturas
                );
                logExtracaoRepository.gravarLogExtracao(logFaturas);
            } catch (final java.sql.SQLException | RuntimeException e) {
                log.warn("⚠️ Falha no processamento de Capa Faturas GraphQL: {}", e.getMessage());
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    ConstantesEntidades.FATURAS_GRAPHQL,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    ConstantesEntidades.STATUS_ERRO_API,
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
            }
        }
        logExtracaoRepository.criarOuAtualizarViewDimFiliais();
        logExtracaoRepository.criarOuAtualizarViewDimClientes();
        logExtracaoRepository.criarOuAtualizarViewDimVeiculos();
        logExtracaoRepository.criarOuAtualizarViewDimMotoristas();
    }

    
}
