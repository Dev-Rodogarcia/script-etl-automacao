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
import br.com.extrator.modelo.graphql.fretes.nfse.NfseNodeDTO;
import br.com.extrator.util.GerenciadorConexao;

/**
 * Runner independente para a API GraphQL (Coletas e Fretes).
 */
public final class GraphQLRunner {

    private GraphQLRunner() {}

    public static void executar(final LocalDate dataInicio) throws Exception {
        System.out.println("🔄 Executando runner GraphQL...");
        br.com.extrator.util.CarregadorConfig.validarConexaoBancoDados();

        final ClienteApiGraphQL clienteApiGraphQL = new ClienteApiGraphQL();
        clienteApiGraphQL.setExecutionUuid(java.util.UUID.randomUUID().toString());
        final ColetaRepository coletaRepository = new ColetaRepository();
        final FreteRepository freteRepository = new FreteRepository();
        final LogExtracaoRepository logExtracaoRepository = new LogExtracaoRepository();

        final ColetaMapper coletaMapper = new ColetaMapper();
        final FreteMapper freteMapper = new FreteMapper();

        // Garante que a tabela log_extracoes existe
        logExtracaoRepository.criarTabelaSeNaoExistir();

        // Coletas
        System.out.println("\n📦 Extraindo Coletas...");
        final LocalDateTime inicioColetas = LocalDateTime.now();
        try {
            final ResultadoExtracao<ColetaNodeDTO> resultadoColetas = clienteApiGraphQL.buscarColetas(dataInicio);
            final List<ColetaNodeDTO> coletasDTO = resultadoColetas.getDados();
            System.out.println("✓ Extraídas: " + coletasDTO.size() + " coletas" + 
                              (resultadoColetas.isCompleto() ? "" : " (INCOMPLETO: " + resultadoColetas.getMotivoInterrupcao() + ")"));

            int registrosSalvos = 0;
            if (!coletasDTO.isEmpty()) {
                final List<ColetaEntity> coletasEntities = coletasDTO.stream()
                    .map(coletaMapper::toEntity)
                    .collect(Collectors.toList());
                registrosSalvos = coletaRepository.salvar(coletasEntities);
                System.out.println("✓ Salvas: " + registrosSalvos + "/" + coletasDTO.size() + " coletas");
            }

            final String status = resultadoColetas.isCompleto() ? "COMPLETO" : "INCOMPLETO";
            final int totalRecebido = coletasDTO.size();
            final int deltaIgnorados = Math.max(0, totalRecebido - registrosSalvos);
            final String mensagem = "API: " + totalRecebido + " recebidos | DB: " + registrosSalvos + " processados | Delta: " + deltaIgnorados + " (duplicados/ignorados)";
            
            final LogExtracaoEntity logColetas = new LogExtracaoEntity(
                "coletas",
                inicioColetas,
                LocalDateTime.now(),
                status,
                registrosSalvos,
                resultadoColetas.getPaginasProcessadas(),
                mensagem
            );
            logExtracaoRepository.gravarLogExtracao(logColetas);

        } catch (RuntimeException | java.sql.SQLException e) {
            // Registrar erro no log
            final LogExtracaoEntity logErro = new LogExtracaoEntity(
                "coletas",
                inicioColetas,
                LocalDateTime.now(),
                "ERRO_API",
                0,
                0,
                "Erro: " + e.getMessage()
            );
            logExtracaoRepository.gravarLogExtracao(logErro);
            throw new RuntimeException("Falha na extração de coletas", e);
        }

        Thread.sleep(2000);

        // Fretes
        System.out.println("\n🚛 Extraindo Fretes...");
        final LocalDateTime inicioFretes = LocalDateTime.now();
        try {
            final ResultadoExtracao<FreteNodeDTO> resultadoFretes = clienteApiGraphQL.buscarFretes(dataInicio);
            final List<FreteNodeDTO> fretesDTO = resultadoFretes.getDados();
            System.out.println("✓ Extraídos: " + fretesDTO.size() + " fretes" + 
                              (resultadoFretes.isCompleto() ? "" : " (INCOMPLETO: " + resultadoFretes.getMotivoInterrupcao() + ")"));

            int registrosSalvos = 0;
            if (!fretesDTO.isEmpty()) {
                final List<FreteEntity> fretesEntities = fretesDTO.stream()
                    .map(freteMapper::toEntity)
                    .collect(Collectors.toList());
                registrosSalvos = freteRepository.salvar(fretesEntities);
                System.out.println("✓ Salvos: " + registrosSalvos + "/" + fretesDTO.size() + " fretes");
            }

            final String status = resultadoFretes.isCompleto() ? "COMPLETO" : "INCOMPLETO";
            final int totalRecebido = fretesDTO.size();
            final int deltaIgnorados = Math.max(0, totalRecebido - registrosSalvos);
            final String mensagem = "API: " + totalRecebido + " recebidos | DB: " + registrosSalvos + " processados | Delta: " + deltaIgnorados + " (duplicados/ignorados)";
            
            final LogExtracaoEntity logFretes = new LogExtracaoEntity(
                "fretes",
                inicioFretes,
                LocalDateTime.now(),
                status,
                registrosSalvos,
                resultadoFretes.getPaginasProcessadas(),
                mensagem
            );
            logExtracaoRepository.gravarLogExtracao(logFretes);

            try {
                final ResultadoExtracao<NfseNodeDTO> nfseResultado = clienteApiGraphQL.buscarNfseDireta(LocalDate.now());
                final java.util.List<NfseNodeDTO> nfseList = nfseResultado.getDados();
                if (!nfseList.isEmpty()) {
                    final int atualizados = freteRepository.atualizarCamposNfse(nfseList);
                    System.out.println("✓ NFSe enriquecimento aplicado: " + atualizados + " fretes atualizados");
                } else {
                    System.out.println("ℹ️ NFSe direta retornou 0 registros para enriquecimento");
                }
            } catch (final java.sql.SQLException | RuntimeException e) {
                System.out.println("⚠️ Falha no enriquecimento NFSe: " + e.getMessage());
            }

            final LocalDateTime inicioFaturas = LocalDateTime.now();
            try {
                final ResultadoExtracao<br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO> capaResultado = clienteApiGraphQL.buscarCapaFaturas(LocalDate.now());
                final java.util.List<br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO> capaList = capaResultado.getDados();
                int registrosSalvosFaturas = 0;
                if (!capaList.isEmpty()) {
                    final br.com.extrator.db.repository.FaturaGraphQLRepository repo = new br.com.extrator.db.repository.FaturaGraphQLRepository();
                    final java.util.List<br.com.extrator.db.entity.FaturaGraphQLEntity> entities = new java.util.ArrayList<>();
                    final com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    for (final br.com.extrator.modelo.graphql.faturas.CreditCustomerBillingNodeDTO dto : capaList) {
                        final br.com.extrator.db.entity.FaturaGraphQLEntity e = new br.com.extrator.db.entity.FaturaGraphQLEntity();
                        e.setId(dto.getId());
                        e.setDocument(dto.getDocument());
                        try { e.setIssueDate(dto.getIssueDate() != null ? java.time.LocalDate.parse(dto.getIssueDate()) : null); } catch (final Exception ignored) {}
                        try { e.setDueDate(dto.getDueDate() != null ? java.time.LocalDate.parse(dto.getDueDate()) : null); } catch (final Exception ignored) {}
                        e.setValue(dto.getValue());
                        if (dto.getCustomer() != null) {
                            try {
                                if (dto.getCustomer().getId() != null) {
                                    try { e.setCustomerId(Long.valueOf(dto.getCustomer().getId())); } catch (final NumberFormatException ex) { e.setCustomerId(null); }
                                }
                            } catch (final Exception ignored) {}
                            String nomeCliente = null;
                            String cnpjCliente = null;
                            if (dto.getCustomer().getPerson() != null) {
                                if (nomeCliente == null) nomeCliente = dto.getCustomer().getPerson().getName();
                                if (cnpjCliente == null) cnpjCliente = dto.getCustomer().getPerson().getCnpj();
                            }
                            if (nomeCliente == null) nomeCliente = dto.getCustomer().getNickname();
                            if (nomeCliente == null) nomeCliente = dto.getCustomer().getName();
                            if (cnpjCliente == null) cnpjCliente = dto.getCustomer().getCnpj();
                            e.setCustomerName(nomeCliente);
                            e.setCustomerCnpj(cnpjCliente);
                        }
                        try { e.setMetadata(om.writeValueAsString(dto)); } catch (final com.fasterxml.jackson.core.JsonProcessingException ex) { e.setMetadata(null); }
                        entities.add(e);
                    }
                    final int salvos = repo.salvar(entities);
                    registrosSalvosFaturas = salvos;
                    System.out.println("✓ Capa Faturas GraphQL salvos: " + salvos + "/" + entities.size());
                    try {
                        final int clientesAtualizados = repo.enriquecerClientesViaJoins();
                        System.out.println("✓ Enriquecimento de clientes aplicado: " + clientesAtualizados + " linhas atualizadas");
                    } catch (final java.sql.SQLException e) {
                        System.out.println("⚠️ Falha no enriquecimento de clientes: " + e.getMessage());
                    }
                    try {
                        final br.com.extrator.db.repository.FaturaPorClienteRepository fpcRepo = new br.com.extrator.db.repository.FaturaPorClienteRepository();
                        final int nfseAtualizadas = fpcRepo.enriquecerNumeroNfseViaTabelaPonte();
                        System.out.println("✓ Relatório Faturas enriquecido com NFS-e: " + nfseAtualizadas + " linhas atualizadas");
                        final int pagadorAtualizadas = fpcRepo.enriquecerPagadorViaTabelaPonte();
                        System.out.println("✓ Relatório Faturas enriquecido com Pagador: " + pagadorAtualizadas + " linhas atualizadas");
                    } catch (final java.sql.SQLException e) {
                        System.out.println("⚠️ Enriquecimento via tabela ponte ignorado: " + e.getMessage());
                    }
                } else {
                    System.out.println("ℹ️ Capa Faturas GraphQL retornou 0 registros");
                }
                final String statusFaturas = capaResultado.isCompleto() ? "COMPLETO" : "INCOMPLETO";
                final int totalRecebidoFaturas = capaList.size();
                final int deltaIgnoradosFaturas = Math.max(0, totalRecebidoFaturas - registrosSalvosFaturas);
                final String msgFaturas = "API: " + totalRecebidoFaturas + " recebidos | DB: " + registrosSalvosFaturas + " processados | Delta: " + deltaIgnoradosFaturas;
                final LogExtracaoEntity logFaturas = new LogExtracaoEntity(
                    "faturas_graphql",
                    inicioFaturas,
                    LocalDateTime.now(),
                    statusFaturas,
                    registrosSalvosFaturas,
                    capaResultado.getPaginasProcessadas(),
                    msgFaturas
                );
                logExtracaoRepository.gravarLogExtracao(logFaturas);
            } catch (final java.sql.SQLException | RuntimeException e) {
                System.out.println("⚠️ Falha no processamento de Capa Faturas GraphQL: " + e.getMessage());
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    "faturas_graphql",
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    "ERRO_API",
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
            }

        } catch (RuntimeException | java.sql.SQLException e) {
            // Registrar erro no log
            final LogExtracaoEntity logErro = new LogExtracaoEntity(
                "fretes",
                inicioFretes,
                LocalDateTime.now(),
                "ERRO_API",
                0,
                0,
                "Erro: " + e.getMessage()
            );
            logExtracaoRepository.gravarLogExtracao(logErro);
            throw new RuntimeException("Falha na extração de fretes", e);
        }
        logExtracaoRepository.criarOuAtualizarViewDimFiliais();
        logExtracaoRepository.criarOuAtualizarViewDimClientes();
        logExtracaoRepository.criarOuAtualizarViewDimVeiculos();
        logExtracaoRepository.criarOuAtualizarViewDimMotoristas();
    }

    public static void executar(final LocalDate dataInicio, final String entidade) throws Exception {
        System.out.println("🔄 Executando runner GraphQL...");
        br.com.extrator.util.CarregadorConfig.validarConexaoBancoDados();

        final ClienteApiGraphQL clienteApiGraphQL = new ClienteApiGraphQL();
        clienteApiGraphQL.setExecutionUuid(java.util.UUID.randomUUID().toString());
        final ColetaRepository coletaRepository = new ColetaRepository();
        final FreteRepository freteRepository = new FreteRepository();
        final LogExtracaoRepository logExtracaoRepository = new LogExtracaoRepository();

        final ColetaMapper coletaMapper = new ColetaMapper();
        final FreteMapper freteMapper = new FreteMapper();

        logExtracaoRepository.criarTabelaSeNaoExistir();

        final boolean executarColetas = entidade == null || entidade.isBlank() || "coletas".equalsIgnoreCase(entidade);
        final boolean executarFretes = entidade == null || entidade.isBlank() || "fretes".equalsIgnoreCase(entidade);
        final boolean executarFaturasGraphql = entidade == null || entidade.isBlank() || "faturas_graphql".equalsIgnoreCase(entidade) || "faturas".equalsIgnoreCase(entidade);

        if (executarColetas) {
            System.out.println("\n📦 Extraindo Coletas...");
            final LocalDateTime inicioColetas = LocalDateTime.now();
            try {
                final ResultadoExtracao<ColetaNodeDTO> resultadoColetas = clienteApiGraphQL.buscarColetas(dataInicio);
                final List<ColetaNodeDTO> coletasDTO = resultadoColetas.getDados();
                System.out.println("✓ Extraídas: " + coletasDTO.size() + " coletas" + 
                                  (resultadoColetas.isCompleto() ? "" : " (INCOMPLETO: " + resultadoColetas.getMotivoInterrupcao() + ")"));
                int registrosSalvos = 0;
                if (!coletasDTO.isEmpty()) {
                    final List<ColetaEntity> coletasEntities = coletasDTO.stream()
                        .map(coletaMapper::toEntity)
                        .collect(Collectors.toList());
                    registrosSalvos = coletaRepository.salvar(coletasEntities);
                    System.out.println("✓ Salvas: " + registrosSalvos + "/" + coletasDTO.size() + " coletas");
                }
                final String status = resultadoColetas.isCompleto() ? "COMPLETO" : "INCOMPLETO";
                final int totalRecebido = coletasDTO.size();
                final int deltaIgnorados = Math.max(0, totalRecebido - registrosSalvos);
                final String mensagem = "API: " + totalRecebido + " recebidos | DB: " + registrosSalvos + " processados | Delta: " + deltaIgnorados + " (duplicados/ignorados)";
                final LogExtracaoEntity logColetas = new LogExtracaoEntity(
                    "coletas",
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
                    "coletas",
                    inicioColetas,
                    LocalDateTime.now(),
                    "ERRO_API",
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
                throw new RuntimeException("Falha na extração de coletas", e);
            }
        }

        if (executarFretes) {
            System.out.println("\n🚛 Extraindo Fretes...");
            final LocalDateTime inicioFretes = LocalDateTime.now();
            try {
                final ResultadoExtracao<FreteNodeDTO> resultadoFretes = clienteApiGraphQL.buscarFretes(dataInicio);
                final List<FreteNodeDTO> fretesDTO = resultadoFretes.getDados();
                System.out.println("✓ Extraídos: " + fretesDTO.size() + " fretes" + 
                                  (resultadoFretes.isCompleto() ? "" : " (INCOMPLETO: " + resultadoFretes.getMotivoInterrupcao() + ")"));
                int registrosSalvos = 0;
                if (!fretesDTO.isEmpty()) {
                    final List<FreteEntity> fretesEntities = fretesDTO.stream()
                        .map(freteMapper::toEntity)
                        .collect(Collectors.toList());
                    registrosSalvos = freteRepository.salvar(fretesEntities);
                    System.out.println("✓ Salvos: " + registrosSalvos + "/" + fretesDTO.size() + " fretes");
                }
                final String status = resultadoFretes.isCompleto() ? "COMPLETO" : "INCOMPLETO";
                final int totalRecebido = fretesDTO.size();
                final int deltaIgnorados = Math.max(0, totalRecebido - registrosSalvos);
                final String mensagem = "API: " + totalRecebido + " recebidos | DB: " + registrosSalvos + " processados | Delta: " + deltaIgnorados + " (duplicados/ignorados)";
                final LogExtracaoEntity logFretes = new LogExtracaoEntity(
                    "fretes",
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
                    "fretes",
                    inicioFretes,
                    LocalDateTime.now(),
                    "ERRO_API",
                    0,
                    0,
                    "Erro: " + e.getMessage()
                );
                logExtracaoRepository.gravarLogExtracao(logErro);
                throw new RuntimeException("Falha na extração de fretes", e);
            }
        }
        
        if (executarFaturasGraphql) {
            System.out.println("\n🧾 Processando Capa Faturas (GraphQL)...");
            try (Connection conn = GerenciadorConexao.obterConexao()) {
                new FaturaGraphQLRepository().criarTabelaSeNaoExistirPublico(conn);
            } catch (final java.sql.SQLException e) {
                System.out.println("⚠️ Falha ao verificar/criar tabela faturas_graphql: " + e.getMessage());
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
                        try { e.setIssueDate(dto.getIssueDate() != null ? java.time.LocalDate.parse(dto.getIssueDate()) : null); } catch (final Exception ignored) {}
                        try { e.setDueDate(dto.getDueDate() != null ? java.time.LocalDate.parse(dto.getDueDate()) : null); } catch (final Exception ignored) {}
                        e.setValue(dto.getValue());
                        if (dto.getCustomer() != null) {
                            try {
                                if (dto.getCustomer().getId() != null) {
                                    try { e.setCustomerId(Long.valueOf(dto.getCustomer().getId())); } catch (final NumberFormatException ex) { e.setCustomerId(null); }
                                }
                            } catch (final Exception ignored) {}
                            String nomeCliente = null;
                            String cnpjCliente = null;
                            if (dto.getCustomer().getPerson() != null) {
                                if (nomeCliente == null) nomeCliente = dto.getCustomer().getPerson().getName();
                                if (cnpjCliente == null) cnpjCliente = dto.getCustomer().getPerson().getCnpj();
                            }
                            if (nomeCliente == null) nomeCliente = dto.getCustomer().getNickname();
                            if (nomeCliente == null) nomeCliente = dto.getCustomer().getName();
                            if (cnpjCliente == null) cnpjCliente = dto.getCustomer().getCnpj();
                            e.setCustomerName(nomeCliente);
                            e.setCustomerCnpj(cnpjCliente);
                        }
                        try { e.setMetadata(om.writeValueAsString(dto)); } catch (final com.fasterxml.jackson.core.JsonProcessingException ex) { e.setMetadata(null); }
                        entities.add(e);
                    }
                    final int salvos = repo.salvar(entities);
                    System.out.println("✓ Capa Faturas GraphQL salvos: " + salvos + "/" + entities.size());
                    try {
                        final int clientesAtualizados = repo.enriquecerClientesViaJoins();
                        System.out.println("✓ Enriquecimento de clientes aplicado: " + clientesAtualizados + " linhas atualizadas");
                    } catch (final java.sql.SQLException e) {
                        System.out.println("⚠️ Falha no enriquecimento de clientes: " + e.getMessage());
                    }
                    try {
                        final br.com.extrator.db.repository.FaturaPorClienteRepository fpcRepo = new br.com.extrator.db.repository.FaturaPorClienteRepository();
                        final int nfseAtualizadas = fpcRepo.enriquecerNumeroNfseViaTabelaPonte();
                        System.out.println("✓ Relatório Faturas enriquecido com NFS-e: " + nfseAtualizadas + " linhas atualizadas");
                        final int pagadorAtualizadas = fpcRepo.enriquecerPagadorViaTabelaPonte();
                        System.out.println("✓ Relatório Faturas enriquecido com Pagador: " + pagadorAtualizadas + " linhas atualizadas");
                    } catch (final java.sql.SQLException e) {
                        System.out.println("⚠️ Enriquecimento via tabela ponte ignorado: " + e.getMessage());
                    }
                } else {
                    System.out.println("ℹ️ Capa Faturas GraphQL retornou 0 registros");
                }
                final String statusFaturas = capaResultado.isCompleto() ? "COMPLETO" : "INCOMPLETO";
                final int totalRecebidoFaturas = capaList.size();
                final int registrosSalvosFaturas = totalRecebidoFaturas; 
                final int deltaIgnoradosFaturas = Math.max(0, totalRecebidoFaturas - registrosSalvosFaturas);
                final String msgFaturas = "API: " + totalRecebidoFaturas + " recebidos | DB: " + registrosSalvosFaturas + " processados | Delta: " + deltaIgnoradosFaturas;
                final LogExtracaoEntity logFaturas = new LogExtracaoEntity(
                    "faturas_graphql",
                    inicioFaturas,
                    LocalDateTime.now(),
                    statusFaturas,
                    registrosSalvosFaturas,
                    capaResultado.getPaginasProcessadas(),
                    msgFaturas
                );
                logExtracaoRepository.gravarLogExtracao(logFaturas);
            } catch (final java.sql.SQLException | RuntimeException e) {
                System.out.println("⚠️ Falha no processamento de Capa Faturas GraphQL: " + e.getMessage());
                final LogExtracaoEntity logErro = new LogExtracaoEntity(
                    "faturas_graphql",
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    "ERRO_API",
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
