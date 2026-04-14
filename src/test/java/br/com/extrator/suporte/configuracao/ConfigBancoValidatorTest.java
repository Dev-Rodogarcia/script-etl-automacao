package br.com.extrator.suporte.configuracao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ConfigBancoValidatorTest {

    @AfterEach
    void limparOverrides() {
        System.clearProperty("etl.integridade.modo");
        System.clearProperty("db.atomic.commit");
        System.clearProperty("etl.environment");
    }

    @Test
    void deveBloquearCommitNaoAtomicoEmModoEstrito() throws Exception {
        System.setProperty("etl.integridade.modo", "STRICT_INTEGRITY");
        System.setProperty("db.atomic.commit", "false");

        final Method method = ConfigBancoValidator.class.getDeclaredMethod("validarConfiguracaoPersistenciaSegura");
        method.setAccessible(true);

        assertThrows(InvocationTargetException.class, () -> method.invoke(null));
    }

    @Test
    void deveAceitarConfiguracaoPadraoSegura() throws Exception {
        System.setProperty("etl.integridade.modo", "STRICT_INTEGRITY");
        System.setProperty("db.atomic.commit", "true");

        final Method method = ConfigBancoValidator.class.getDeclaredMethod("validarConfiguracaoPersistenciaSegura");
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(null));
    }
}
