package br.com.extrator.plataforma.auditoria.persistencia.sqlserver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SqlServerExecutionAuditPortAdapterTest {

    @AfterEach
    void limparOverrides() {
        System.clearProperty("etl.integridade.modo");
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
        final SqlServerExecutionAuditPortAdapter adapter = new SqlServerExecutionAuditPortAdapter();

        final Method method = SqlServerExecutionAuditPortAdapter.class.getDeclaredMethod(
            "tratarFalhaEscrita",
            String.class
        );
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(adapter, "falha simulada"));
    }
}
