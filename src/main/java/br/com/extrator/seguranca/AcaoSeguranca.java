package br.com.extrator.seguranca;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Acoes protegidas por autenticacao/autorizacao.
 */
public enum AcaoSeguranca {
    RUN_EXTRACAO_COMPLETA(EnumSet.of(PerfilAcesso.ADMIN, PerfilAcesso.OPERADOR), "Executar extracao completa"),
    RUN_EXTRACAO_INTERVALO(EnumSet.of(PerfilAcesso.ADMIN, PerfilAcesso.OPERADOR), "Executar extracao por intervalo"),
    RUN_TESTAR_API(EnumSet.of(PerfilAcesso.ADMIN, PerfilAcesso.OPERADOR), "Testar API especifica"),
    RUN_VALIDAR_CONFIG(EnumSet.of(PerfilAcesso.ADMIN, PerfilAcesso.OPERADOR), "Validar configuracoes"),
    RUN_RELATORIO_VALIDACAO(EnumSet.of(PerfilAcesso.ADMIN, PerfilAcesso.OPERADOR), "Executar relatorio completo"),
    RUN_EXPORTAR_CSV(EnumSet.of(PerfilAcesso.ADMIN, PerfilAcesso.OPERADOR), "Exportar CSV"),
    RUN_AUDITORIA_API(EnumSet.of(PerfilAcesso.ADMIN, PerfilAcesso.OPERADOR), "Auditar estrutura das APIs"),
    RUN_AJUDA(EnumSet.of(PerfilAcesso.ADMIN, PerfilAcesso.OPERADOR, PerfilAcesso.VISUALIZADOR), "Visualizar ajuda"),
    LOOP_START(EnumSet.of(PerfilAcesso.ADMIN, PerfilAcesso.OPERADOR), "Iniciar loop em segundo plano"),
    LOOP_STOP(EnumSet.of(PerfilAcesso.ADMIN), "Parar loop em segundo plano"),
    LOOP_STATUS(EnumSet.of(PerfilAcesso.ADMIN, PerfilAcesso.OPERADOR, PerfilAcesso.VISUALIZADOR), "Consultar status do loop"),
    LOOP_EXIT_MENU(EnumSet.of(PerfilAcesso.ADMIN, PerfilAcesso.OPERADOR, PerfilAcesso.VISUALIZADOR), "Sair do menu do loop"),
    MENU_EXIT(EnumSet.of(PerfilAcesso.ADMIN, PerfilAcesso.OPERADOR, PerfilAcesso.VISUALIZADOR), "Sair do menu principal"),
    AUTH_CREATE_USER(EnumSet.of(PerfilAcesso.ADMIN), "Criar usuario"),
    AUTH_RESET_PASSWORD(EnumSet.of(PerfilAcesso.ADMIN), "Redefinir senha de usuario"),
    AUTH_DISABLE_USER(EnumSet.of(PerfilAcesso.ADMIN), "Desativar usuario");

    private final Set<PerfilAcesso> perfisPermitidos;
    private final String descricao;

    AcaoSeguranca(final Set<PerfilAcesso> perfisPermitidos, final String descricao) {
        this.perfisPermitidos = perfisPermitidos;
        this.descricao = descricao;
    }

    public boolean permite(final PerfilAcesso perfilAcesso) {
        return perfisPermitidos.contains(perfilAcesso);
    }

    public String getDescricao() {
        return descricao;
    }

    public static AcaoSeguranca fromToken(final String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Acao de seguranca nao informada.");
        }
        try {
            return AcaoSeguranca.valueOf(token.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException e) {
            final String validos = Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Acao de seguranca invalida: " + token + ". Acoes validas: " + validos);
        }
    }
}
