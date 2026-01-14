package br.com.extrator.api.constantes;

import java.util.Map;

import br.com.extrator.api.graphql.GraphQLQueries;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Constantes centralizadas para a API GraphQL do ESL Cloud.
 * Utiliza Record para agrupar todas as configurações de cada entidade,
 * facilitando manutenção e comparação com Insomnia.
 * 
 * Para adicionar nova entidade: apenas adicionar entrada no Map CONFIGURACOES.
 * 
 * @author Sistema de Extração ESL Cloud
 * @version 1.0
 */
public final class ConstantesApiGraphQL {

    private ConstantesApiGraphQL() {
        // Impede instanciação
    }

    // ========== ENDPOINT ==========
    /**
     * Endpoint para requisições GraphQL.
     */
    public static final String ENDPOINT_GRAPHQL = "/graphql";

    /**
     * Template ID usado para auditoria de páginas GraphQL.
     */
    public static final int TEMPLATE_ID_AUDIT = 9901;

    // ========== RECORD DE CONFIGURAÇÃO ==========
    /**
     * Record que agrupa todas as configurações de uma entidade GraphQL.
     * Imutável e type-safe.
     * 
     * @param campoFiltro Nome do campo de filtro na query (ex: "requestDate", "serviceAt")
     * @param query Query GraphQL completa (de GraphQLQueries)
     * @param nomeEntidadeApi Nome da entidade na resposta da API (ex: "pick", "freight")
     * @param suportaIntervalo Se true, aceita intervalo de datas no filtro; se false, aceita apenas data única
     */
    public record ConfiguracaoGraphQL(
        String campoFiltro,
        String query,
        String nomeEntidadeApi,
        boolean suportaIntervalo
    ) {}

    // ========== MAPA DE CONFIGURAÇÕES ==========
    /**
     * Mapa de configurações por entidade.
     * Chave: ConstantesEntidades.* (ex: "coletas", "fretes")
     * Valor: ConfiguracaoGraphQL com todas as configurações
     */
    private static final Map<String, ConfiguracaoGraphQL> CONFIGURACOES = Map.of(
        // COLETAS - Pick
        ConstantesEntidades.COLETAS,
        new ConfiguracaoGraphQL(
            "requestDate",                  // campoFiltro (aceita apenas data única)
            GraphQLQueries.QUERY_COLETAS,   // query
            "pick",                         // nomeEntidadeApi
            false                           // suportaIntervalo (NÃO suporta)
        ),

        // FRETES - Freight
        ConstantesEntidades.FRETES,
        new ConfiguracaoGraphQL(
            "serviceAt",                    // campoFiltro (aceita intervalo)
            GraphQLQueries.QUERY_FRETES,    // query
            "freight",                      // nomeEntidadeApi
            true                            // suportaIntervalo (SUPORTA)
        ),

        // FATURAS_GRAPHQL - CreditCustomerBilling
        ConstantesEntidades.FATURAS_GRAPHQL,
        new ConfiguracaoGraphQL(
            "dueDate",                      // campoFiltro (aceita apenas data única)
            GraphQLQueries.QUERY_FATURAS,   // query
            "creditCustomerBilling",        // nomeEntidadeApi
            false                           // suportaIntervalo (NÃO suporta)
        ),

        // USUARIOS_SISTEMA - Individual
        ConstantesEntidades.USUARIOS_SISTEMA,
        new ConfiguracaoGraphQL(
            null,                           // campoFiltro (não usa filtro de data, usa enabled: true)
            GraphQLQueries.QUERY_USUARIOS_SISTEMA, // query
            "individual",                    // nomeEntidadeApi
            false                           // suportaIntervalo (NÃO suporta)
        )
    );

    /**
     * Configuração para NFSe (não está em ConstantesEntidades, então tratamos separadamente).
     */
    private static final ConfiguracaoGraphQL CONFIG_NFSE = new ConfiguracaoGraphQL(
        "issuedAt",                     // campoFiltro (aceita intervalo)
        GraphQLQueries.QUERY_NFSE,      // query
        "nfse",                         // nomeEntidadeApi
        true                            // suportaIntervalo (SUPORTA)
    );

    // ========== MÉTODO PRINCIPAL ==========
    /**
     * Obtém a configuração completa de uma entidade GraphQL.
     * 
     * @param entidade Nome da entidade (usar ConstantesEntidades.*)
     * @return ConfiguracaoGraphQL com todas as configurações
     * @throws IllegalArgumentException se a entidade não for encontrada
     */
    public static ConfiguracaoGraphQL obterConfiguracao(final String entidade) {
        // Tratamento especial para NFSe
        if ("nfse".equalsIgnoreCase(entidade)) {
            return CONFIG_NFSE;
        }
        
        final ConfiguracaoGraphQL config = CONFIGURACOES.get(entidade);
        if (config == null) {
            throw new IllegalArgumentException("Entidade não configurada para GraphQL: " + entidade);
        }
        return config;
    }

    /**
     * Verifica se uma entidade está configurada para GraphQL.
     * 
     * @param entidade Nome da entidade
     * @return true se a entidade possui configuração
     */
    public static boolean possuiConfiguracao(final String entidade) {
        return CONFIGURACOES.containsKey(entidade) || "nfse".equalsIgnoreCase(entidade);
    }

    // ========== MÉTODOS AUXILIARES (CONVENIÊNCIA) ==========
    /**
     * Obtém o campo de filtro de uma entidade.
     */
    public static String obterCampoFiltro(final String entidade) {
        return obterConfiguracao(entidade).campoFiltro();
    }

    /**
     * Obtém a query GraphQL de uma entidade.
     */
    public static String obterQuery(final String entidade) {
        return obterConfiguracao(entidade).query();
    }

    /**
     * Obtém o nome da entidade na API.
     */
    public static String obterNomeEntidadeApi(final String entidade) {
        return obterConfiguracao(entidade).nomeEntidadeApi();
    }

    /**
     * Verifica se a entidade suporta intervalo de datas no filtro.
     */
    public static boolean suportaIntervalo(final String entidade) {
        return obterConfiguracao(entidade).suportaIntervalo();
    }
}

