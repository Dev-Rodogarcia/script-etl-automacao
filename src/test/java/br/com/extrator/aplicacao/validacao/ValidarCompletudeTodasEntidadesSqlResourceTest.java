package br.com.extrator.aplicacao.validacao;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class ValidarCompletudeTodasEntidadesSqlResourceTest {

    @Test
    void deveCobrirTodasAsEntidadesOperacionaisNoSqlLegado() throws IOException {
        final String sql = carregarSql();

        for (final String entidade : List.of(
            "usuarios_sistema",
            "coletas",
            "fretes",
            "faturas_graphql",
            "manifestos",
            "cotacoes",
            "localizacao_cargas",
            "contas_a_pagar",
            "faturas_por_cliente",
            "inventario",
            "sinistros"
        )) {
            assertTrue(sql.contains("'" + entidade + "'"), "Entidade ausente do SQL de completude: " + entidade);
        }

        assertTrue(sql.contains("i.data_extracao"), "Inventario deve ser contado pela janela de data_extracao.");
        assertTrue(sql.contains("s.data_extracao"), "Sinistros deve ser contado pela janela de data_extracao.");
        assertTrue(sql.contains("timestamp_inicio"), "A consulta deve usar a janela da ultima extracao.");
        assertTrue(sql.contains("timestamp_fim"), "A consulta deve usar a janela da ultima extracao.");
    }

    private String carregarSql() throws IOException {
        try (InputStream input = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("sql/validacao/validar_completude_todas_entidades.sql")) {
            assertTrue(input != null, "Recurso SQL de completude nao encontrado.");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
