package br.com.extrator.integracao;

/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/integracao/GraphQLLookupSupport.java
Classe  :  (class)
Pacote  : br.com.extrator.integracao
Modulo  : Integracao HTTP
Papel   : [DESC PENDENTE]
Conecta com: Sem dependencia interna
Fluxo geral:
1) [PENDENTE]
Estrutura interna:
Metodos: [PENDENTE]
Atributos: [PENDENTE]
[DOC-FILE-END]============================================================== */


import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import br.com.extrator.integracao.graphql.GraphQLQueries;
import br.com.extrator.dominio.graphql.bancos.BankAccountNodeDTO;

final class GraphQLLookupSupport {
    @FunctionalInterface
    interface TypedQueryExecutor {
        <T> PaginatedGraphQLResponse<T> executar(
            String query,
            String nomeEntidade,
            Map<String, Object> variaveis,
            Class<T> tipoClasse
        );
    }

    private final TypedQueryExecutor queryExecutor;
    private final Logger logger;

    GraphQLLookupSupport(final TypedQueryExecutor queryExecutor, final Logger logger) {
        this.queryExecutor = queryExecutor;
        this.logger = logger;
    }

    Optional<BankAccountNodeDTO> buscarDetalhesBanco(final Integer bankAccountId) {
        if (bankAccountId == null) {
            logger.warn("Tentativa de buscar detalhes de banco com ID nulo");
            return Optional.empty();
        }

        return buscarPrimeiraEntidade(
            "buscar detalhes de banco",
            "bankAccount",
            GraphQLQueries.QUERY_RESOLVER_CONTA_BANCARIA,
            Map.of("id", bankAccountId),
            BankAccountNodeDTO.class,
            String.valueOf(bankAccountId)
        );
    }

    private <T> Optional<T> buscarPrimeiraEntidade(final String operacao,
                                                   final String nomeEntidade,
                                                   final String query,
                                                   final Map<String, Object> variaveis,
                                                   final Class<T> tipoClasse,
                                                   final String identificador) {
        try {
            final PaginatedGraphQLResponse<T> resposta = queryExecutor.executar(
                query,
                nomeEntidade,
                variaveis,
                tipoClasse
            );

            if (resposta.isErroApi()) {
                logger.warn("Falha ao {} {}: {}", operacao, identificador, resposta.getErroDetalhe());
                return Optional.empty();
            }

            if (resposta.getEntidades() != null && !resposta.getEntidades().isEmpty()) {
                return Optional.of(resposta.getEntidades().get(0));
            }
            return Optional.empty();
        } catch (final Exception e) {
            logger.error("Erro ao {} {}: {}", operacao, identificador, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
