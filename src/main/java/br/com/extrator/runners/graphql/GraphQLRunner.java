package br.com.extrator.runners.graphql;

import java.time.LocalDate;

import br.com.extrator.runners.graphql.services.GraphQLExtractionService;
import br.com.extrator.util.console.LoggerConsole;

/**
 * Runner independente para a API GraphQL (Coletas, Fretes e Faturas GraphQL).
 * Refatorado para usar serviços de orquestração.
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
        
        final GraphQLExtractionService service = new GraphQLExtractionService();
        service.execute(dataInicio, dataFim, entidade);
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
        
        final GraphQLExtractionService service = new GraphQLExtractionService();
        service.execute(dataInicio, dataInicio, entidade);
    }
    
    /**
     * FASE 3: Executa extração APENAS de Faturas GraphQL para um intervalo de datas.
     * Este método é chamado APÓS todas as outras entidades serem extraídas.
     * 
     * Motivo: O enriquecimento de faturas_graphql é muito demorado (50+ minutos),
     * então as outras entidades são priorizadas para garantir dados parciais atualizados no BI.
     * 
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @throws Exception Se houver falha na extração
     */
    public static void executarFaturasGraphQLPorIntervalo(final LocalDate dataInicio, final LocalDate dataFim) throws Exception {
        log.info("🔄 [FASE 3] Executando extração de Faturas GraphQL por último...");
        log.info("📅 Período: {} a {}", dataInicio, dataFim);
        
        final GraphQLExtractionService service = new GraphQLExtractionService();
        service.execute(dataInicio, dataFim, br.com.extrator.util.validacao.ConstantesEntidades.FATURAS_GRAPHQL);
    }
}
