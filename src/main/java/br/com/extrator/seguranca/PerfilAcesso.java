package br.com.extrator.seguranca;

import java.util.Locale;

/**
 * Perfis de acesso do modulo de seguranca operacional.
 */
public enum PerfilAcesso {
    ADMIN,
    OPERADOR,
    VISUALIZADOR;

    public static PerfilAcesso fromString(final String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("Perfil de acesso nao informado.");
        }
        try {
            return PerfilAcesso.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Perfil de acesso invalido: " + valor);
        }
    }
}
