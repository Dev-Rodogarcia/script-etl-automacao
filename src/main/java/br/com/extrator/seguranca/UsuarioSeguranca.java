package br.com.extrator.seguranca;

import java.time.LocalDateTime;

/**
 * Modelo de usuario para autenticacao operacional.
 */
public record UsuarioSeguranca(
    long id,
    String username,
    String displayName,
    PerfilAcesso perfilAcesso,
    boolean ativo,
    int tentativasFalhas,
    LocalDateTime bloqueadoAte,
    String senhaHashBase64,
    String senhaSaltBase64
) {
}
