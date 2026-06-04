package br.com.extrator.integracao.graphql;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class GraphQLQueriesContractTest {

    @Test
    void queryUsuariosSistemaDeveUsarVariavelAfterCompativelComPaginador() {
        assertTrue(GraphQLQueries.QUERY_USUARIOS_SISTEMA.contains("$after: String"));
        assertTrue(GraphQLQueries.QUERY_USUARIOS_SISTEMA.contains("after: $after"));
        assertFalse(GraphQLQueries.QUERY_USUARIOS_SISTEMA.contains("updatedAt"));
        assertTrue(GraphQLQueries.QUERY_USUARIOS_SISTEMA.contains("id"));
        assertTrue(GraphQLQueries.QUERY_USUARIOS_SISTEMA.contains("name"));
    }
}
