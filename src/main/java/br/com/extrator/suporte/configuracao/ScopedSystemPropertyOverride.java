package br.com.extrator.suporte.configuracao;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aplica overrides temporarios em System properties e restaura o estado anterior ao encerrar o escopo.
 */
public final class ScopedSystemPropertyOverride implements AutoCloseable {
    private final Map<String, String> previousValues = new LinkedHashMap<>();

    private ScopedSystemPropertyOverride(final Map<String, String> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return;
        }
        overrides.forEach((key, value) -> {
            previousValues.put(key, System.getProperty(key));
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
        });
    }

    public static ScopedSystemPropertyOverride apply(final Map<String, String> overrides) {
        return new ScopedSystemPropertyOverride(overrides);
    }

    @Override
    public void close() {
        previousValues.forEach((key, previousValue) -> {
            if (previousValue == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, previousValue);
            }
        });
    }
}
