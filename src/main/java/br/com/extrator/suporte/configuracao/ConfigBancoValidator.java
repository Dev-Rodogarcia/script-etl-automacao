package br.com.extrator.suporte.configuracao;

/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/suporte/configuracao/ConfigBancoValidator.java
Classe  :  (class)
Pacote  : br.com.extrator.suporte.configuracao
Modulo  : Suporte - Config
Papel   : [DESC PENDENTE]
Conecta com: Sem dependencia interna
Fluxo geral:
1) [PENDENTE]
Estrutura interna:
Metodos: [PENDENTE]
Atributos: [PENDENTE]
[DOC-FILE-END]============================================================== */


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ConfigBancoValidator {
    private static final Logger logger = LoggerFactory.getLogger(ConfigBancoValidator.class);
    private static final List<ObjetoBanco> OBJETOS_PRODUCAO_MINIMOS = List.of(
        tabela("coletas"),
        tabela("fretes"),
        tabela("manifestos"),
        tabela("cotacoes"),
        tabela("localizacao_cargas"),
        tabela("contas_a_pagar"),
        tabela("faturas_por_cliente"),
        tabela("dim_calendario"),
        tabela("log_extracoes"),
        tabela("page_audit"),
        tabela("dim_usuarios"),
        tabela("sys_execution_history"),
        tabela("sys_auditoria_temp"),
        tabela("sys_execution_audit"),
        tabela("sys_execution_watermark"),
        tabela("dim_usuarios_historico"),
        tabela("schema_migrations"),
        tabela("etl_invalid_records"),
        tabela("inventario"),
        tabela("sinistros"),
        tabela("sys_replay_idempotency"),
        tabela("sys_reconciliation_quarantine"),
        tabela("raster_viagens"),
        tabela("raster_viagem_paradas"),
        tabela("localizacao_cargas_regiao_destino_alias"),
        tabela("manifestos_frota_propria_cnpjs"),
        tabela("regras_atribuicao_filial"),
        tabela("fato_gestao_vista_fretes"),
        tabela("fato_gestao_vista_coletores"),
        tabela("fato_fretes_faturamento"),
        tabela("fato_gestao_vista_faturas"),
        procedure("sp_carga_fato_gestao_vista_fretes"),
        procedure("sp_carga_fato_gestao_vista_coletores"),
        procedure("sp_carga_fato_fretes_faturamento"),
        procedure("sp_carga_fato_gestao_vista_faturas")
    );

    private ConfigBancoValidator() {
    }

    static void validarConexaoBancoDados() {
        logger.info("Validando conexao com o banco de dados...");
        validarConfiguracaoPersistenciaSegura();

        final String url = ConfigBanco.obterUrlBancoDados();
        final String usuario = ConfigBanco.obterUsuarioBancoDados();
        final String senha = ConfigBanco.obterSenhaBancoDados();

        if (url == null || url.trim().isEmpty()) {
            logger.error("URL do banco de dados nao configurada");
            throw new RuntimeException("Configuracao invalida: URL do banco de dados nao pode estar vazia");
        }
        if (usuario == null || usuario.trim().isEmpty()) {
            logger.error("Usuario do banco de dados nao configurado");
            throw new RuntimeException("Configuracao invalida: Usuario do banco de dados nao pode estar vazio");
        }
        if (senha == null || senha.trim().isEmpty()) {
            logger.error("Senha do banco de dados nao configurada");
            throw new RuntimeException("Configuracao invalida: Senha do banco de dados nao pode estar vazia");
        }

        final int timeoutSegundos = ConfigBanco.obterTimeoutValidacaoConexao();
        try (Connection conexao = obterConexaoValidacao(url, usuario, senha, timeoutSegundos)) {
            if (conexao.isValid(timeoutSegundos)) {
                logger.info("Conexao com banco de dados validada com sucesso (via JDBC)");
                return;
            }
            logger.error("Conexao com banco de dados invalida (via JDBC)");
            throw new RuntimeException("Falha na validacao: Conexao com banco de dados invalida");
        } catch (final SQLException e) {
            logger.error("Erro ao conectar com o banco de dados: {}", e.getMessage());
            throw criarErroConexaoBanco(e);
        } catch (final RuntimeException t) {
            logger.error("Falha ao validar conexao com o banco: {}", t.getMessage());
            throw criarErroConexaoBanco(t);
        }
    }

    static void validarTabelasEssenciais() {
        logger.info("Validando existencia de tabelas essenciais no banco de dados...");

        final String[] tabelasEssenciais = {
            "log_extracoes",
            "page_audit",
            "dim_usuarios"
        };

        final StringBuilder tabelasFaltando = new StringBuilder();
        try (Connection conexao = obterConexaoValidacao(
                ConfigBanco.obterUrlBancoDados(),
                ConfigBanco.obterUsuarioBancoDados(),
                ConfigBanco.obterSenhaBancoDados(),
                ConfigBanco.obterTimeoutValidacaoConexao())) {
            for (final String tabela : tabelasEssenciais) {
                if (!tabelaExiste(conexao, tabela)) {
                    if (tabelasFaltando.length() > 0) {
                        tabelasFaltando.append(", ");
                    }
                    tabelasFaltando.append(tabela);
                }
            }

            if (tabelasFaltando.length() > 0) {
                final String mensagem = String.format(
                    "ERRO CRITICO: As seguintes tabelas nao existem no banco de dados: %s. Execute 'database/executar_database.bat' antes de rodar a aplicacao. Veja database/README.md para instrucoes.",
                    tabelasFaltando
                );
                logger.error(mensagem);
                throw new IllegalStateException(mensagem);
            }

            logger.info("Todas as tabelas essenciais existem no banco de dados");
        } catch (final SQLException e) {
            logger.error("Erro ao validar tabelas essenciais: {}", e.getMessage());
            throw new RuntimeException("Falha ao validar tabelas essenciais", e);
        }
    }

    static void validarObjetosProducaoMinimos() {
        logger.info("Validando objetos minimos de producao no banco de dados...");

        final List<String> ausentes = new ArrayList<>();
        try (Connection conexao = obterConexaoValidacao(
                ConfigBanco.obterUrlBancoDados(),
                ConfigBanco.obterUsuarioBancoDados(),
                ConfigBanco.obterSenhaBancoDados(),
                ConfigBanco.obterTimeoutValidacaoConexao())) {
            for (final ObjetoBanco objeto : OBJETOS_PRODUCAO_MINIMOS) {
                if (!objetoExiste(conexao, objeto)) {
                    ausentes.add(objeto.descricao());
                }
            }
        } catch (final SQLException e) {
            logger.error("Erro ao validar objetos minimos de producao: {}", e.getMessage());
            throw new RuntimeException("Falha ao validar objetos minimos de producao", e);
        }

        if (!ausentes.isEmpty()) {
            throw new IllegalStateException(
                "Schema manual incompleto. Objetos ausentes: " + String.join(", ", ausentes)
                    + ". Execute o DDL manualmente antes de rodar automacoes de carga."
            );
        }

        logger.info("Objetos minimos de producao validados com sucesso");
    }

    private static Connection obterConexaoValidacao(final String url,
                                                    final String usuario,
                                                    final String senha,
                                                    final int timeoutSegundos) throws SQLException {
        DriverManager.setLoginTimeout(Math.max(1, timeoutSegundos));
        return DriverManager.getConnection(obterUrlComDatabaseName(url), usuario, senha);
    }

    private static String obterUrlComDatabaseName(final String urlOriginal) {
        if (urlOriginal == null) {
            return null;
        }

        String url = urlOriginal.trim();
        if (url.startsWith("jdbc:sqlserver://")) {
            final String urlLower = url.toLowerCase();
            final boolean temDatabaseName = urlLower.contains("databasename=");
            final boolean temDatabase = urlLower.contains("database=");
            if (!temDatabaseName && !temDatabase) {
                final String nomeBanco = ConfigBanco.obterNomeBancoDados();
                if (nomeBanco != null && !nomeBanco.trim().isEmpty()) {
                    url = url.endsWith(";")
                        ? url + "databaseName=" + nomeBanco.trim()
                        : url + ";databaseName=" + nomeBanco.trim();
                }
            }
        }
        return url;
    }

    private static boolean tabelaExiste(final Connection conexao, final String nomeTabela) throws SQLException {
        final String sql = """
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = ?
            """;

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, nomeTabela);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static boolean objetoExiste(final Connection conexao, final ObjetoBanco objeto) throws SQLException {
        final String sql = "SELECT CASE WHEN OBJECT_ID(?, ?) IS NULL THEN 0 ELSE 1 END";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, objeto.nomeQualificado());
            stmt.setString(2, objeto.tipoSqlServer());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }

    private static ObjetoBanco tabela(final String nome) {
        return new ObjetoBanco("TABLE", "dbo", nome, "U");
    }

    private static ObjetoBanco procedure(final String nome) {
        return new ObjetoBanco("PROCEDURE", "dbo", nome, "P");
    }

    private static RuntimeException criarErroConexaoBanco(final Throwable causa) {
        final String detalhe = extrairMensagemMaisInterna(causa);
        final String detalheLower = detalhe.toLowerCase();

        String mensagemErro = "Erro de conexao com banco de dados: ";
        if (detalheLower.contains("login failed")) {
            mensagemErro += "Credenciais invalidas (usuario ou senha incorretos)";
        } else if (detalheLower.contains("cannot open database")) {
            mensagemErro += "Banco de dados nao encontrado ou inacessivel";
        } else if (detalheLower.contains("tcp/ip")
                || detalheLower.contains("connection refused")
                || detalheLower.contains("connect timed out")
                || detalheLower.contains("connection reset")) {
            mensagemErro += "Servidor de banco de dados inacessivel (verifique URL, porta, firewall e servico SQL Server)";
        } else {
            mensagemErro += detalhe;
        }

        return new RuntimeException(mensagemErro, causa);
    }

    private static void validarConfiguracaoPersistenciaSegura() {
        if (!ConfigEtl.isModoIntegridadeEstrito() && !isAmbienteProducao()) {
            return;
        }
        if (!ConfigBanco.isModoCommitAtomico()) {
            throw new IllegalStateException(
                "Configuracao insegura para persistencia detectada: db.atomic.commit=false nao e permitido em STRICT_INTEGRITY/PRODUCAO."
            );
        }
        if (!ConfigBanco.isModoCommitAtomico() && ConfigBanco.isContinuarAposErro()) {
            throw new IllegalStateException(
                "Configuracao insegura para persistencia detectada: db.continue.on.error=true com commit nao atomico."
            );
        }
    }

    private static boolean isAmbienteProducao() {
        final String valor = ConfigSource.obterConfiguracao("ETL_ENVIRONMENT", "etl.environment");
        if (valor == null || valor.isBlank()) {
            return false;
        }
        final String normalizado = valor.trim().toLowerCase();
        return "prod".equals(normalizado) || "production".equals(normalizado) || "producao".equals(normalizado);
    }

    private static String extrairMensagemMaisInterna(final Throwable throwable) {
        Throwable atual = throwable;
        while (atual.getCause() != null && atual.getCause() != atual) {
            atual = atual.getCause();
        }
        final String mensagem = atual.getMessage();
        return (mensagem == null || mensagem.isBlank()) ? atual.getClass().getSimpleName() : mensagem;
    }

    private record ObjetoBanco(String tipo, String schema, String nome, String tipoSqlServer) {
        String nomeQualificado() {
            return schema + "." + nome;
        }

        String descricao() {
            return tipo + " " + nomeQualificado();
        }
    }
}
