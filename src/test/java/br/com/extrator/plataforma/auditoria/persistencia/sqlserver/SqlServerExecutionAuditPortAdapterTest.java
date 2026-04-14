package br.com.extrator.plataforma.auditoria.persistencia.sqlserver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqlServerExecutionAuditPortAdapterTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void limparOverrides() {
        System.clearProperty("etl.integridade.modo");
        System.clearProperty("etl.audit.fallback.file");
    }

    @Test
    void deveFalharNoModoEstritoQuandoEscritaDeAuditoriaNaoForPossivel() throws Exception {
        System.setProperty("etl.integridade.modo", "STRICT_INTEGRITY");
        final SqlServerExecutionAuditPortAdapter adapter = new SqlServerExecutionAuditPortAdapter();

        final Method method = SqlServerExecutionAuditPortAdapter.class.getDeclaredMethod(
            "tratarFalhaEscrita",
            String.class
        );
        method.setAccessible(true);

        assertThrows(Exception.class, () -> method.invoke(adapter, "falha simulada"));
    }

    @Test
    void naoDeveFalharForaDoModoEstritoQuandoEscritaDeAuditoriaNaoForPossivel() throws Exception {
        System.setProperty("etl.integridade.modo", "OPERACIONAL");
        final Path fallback = tempDir.resolve("audit-fallback.jsonl");
        System.setProperty("etl.audit.fallback.file", fallback.toString());
        final SqlServerExecutionAuditPortAdapter adapter = new SqlServerExecutionAuditPortAdapter();

        final Method method = SqlServerExecutionAuditPortAdapter.class.getDeclaredMethod(
            "tratarFalhaEscrita",
            String.class
        );
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(adapter, "falha simulada"));
        assertTrue(Files.exists(fallback));
        assertTrue(Files.readString(fallback).contains("falha simulada"));
    }
}
