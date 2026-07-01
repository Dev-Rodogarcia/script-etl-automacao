package br.com.extrator.analises;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FretesPerformanceIndexSqlTest {

    @Test
    void indiceDePerformanceDeveManterMigrationBaselineInstaladorEValidacaoAlinhados() throws IOException {
        String migration = ler("database/migrations/040_criar_indice_performance_fretes.sql");
        String indices = ler("database/indices/001_criar_indices_performance.sql");
        String executor = ler("database/executar_database.bat");
        String validacao = ler("database/validacao/043_validar_indice_performance_fretes.sql");
        String validacaoRecriacao = ler("database/validacao/034_validar_schema_recriacao.sql");

        for (String sql : new String[] {migration, indices}) {
            assertTrue(sql.contains("IX_fretes_performance_minuta_cobertura"));
            assertTrue(sql.contains("ON dbo.fretes(corporation_sequence_number)"));
            assertTrue(sql.contains("data_previsao_entrega"));
            assertTrue(sql.contains("fit_dpn_performance_finished_at"));
            assertTrue(sql.contains("pagador_nome"));
            assertTrue(sql.contains("filial_nome"));
            assertTrue(sql.contains("destino_cidade"));
        }

        assertTrue(executor.contains("dir /b /a-d /on \"migrations\\*.sql\""));
        assertFalse(executor.contains("migrations\\040_criar_indice_performance_fretes.sql"));
        assertTrue(executor.contains("validacao\\043_validar_indice_performance_fretes.sql"));
        assertTrue(validacao.contains("key_ordinal = 1"));
        assertTrue(validacao.contains("ic.is_included_column = 1"));
        assertTrue(validacaoRecriacao.contains("040_criar_indice_performance_fretes"));
        assertTrue(validacaoRecriacao.contains("IX_fretes_performance_minuta_cobertura"));
        assertFalse(migration.contains("\uFFFD"));
        assertFalse(indices.contains("\uFFFD"));
    }

    private static String ler(String caminho) throws IOException {
        Path path = Path.of(caminho);
        if (!Files.exists(path) && caminho.startsWith("database/migrations/")) {
            path = Path.of(caminho.replace(
                "database/migrations/",
                "database/migrations/historico_arquivado/"
            ));
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
