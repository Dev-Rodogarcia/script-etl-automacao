package br.com.extrator.util.configuracao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.util.banco.GerenciadorConexao;

/**
 * Classe responsável por carregar as configurações do arquivo config.properties.
 * 
 * Thread-safe: utiliza o padrão Initialization-on-demand holder para
 * garantir carregamento único e seguro das propriedades.
 * 
 * @author Sistema de Extração ESL Cloud
 * @version 2.0 - Thread-safe
 */
public final class CarregadorConfig {
    private static final Logger logger = LoggerFactory.getLogger(CarregadorConfig.class);
    private static final String ARQUIVO_CONFIG = "config.properties";

    /**
     * Holder pattern para carregamento lazy e thread-safe das propriedades.
     * A JVM garante que a classe interna só será carregada quando acessada,
     * e o carregamento de classe é thread-safe por especificação.
     */
    private static final class PropertiesHolder {
        private static final Properties INSTANCE = carregarPropriedadesInterno();

        private static Properties carregarPropriedadesInterno() {
            final Properties props = new Properties();
            try (InputStream input = CarregadorConfig.class.getClassLoader().getResourceAsStream(ARQUIVO_CONFIG)) {
                if (input == null) {
                    logger.error("Não foi possível encontrar o arquivo {}", ARQUIVO_CONFIG);
                    throw new RuntimeException("Arquivo de configuração não encontrado: " + ARQUIVO_CONFIG);
                }
                props.load(input);
                logger.info("Arquivo de configuração carregado com sucesso (thread-safe)");
            } catch (final IOException ex) {
                logger.error("Erro ao carregar o arquivo de configuração", ex);
                throw new RuntimeException("Erro ao carregar arquivo de configuração", ex);
            }
            return props;
        }
    }

    private CarregadorConfig() {
        // Impede instanciação
    }

    /**
     * Carrega as propriedades do arquivo de configuração.
     * Thread-safe e lazy - carrega apenas no primeiro acesso.
     * 
     * @return Objeto Properties com as configurações carregadas
     */
    public static Properties carregarPropriedades() {
        return PropertiesHolder.INSTANCE;
    }

    /**
     * Valida a conexão com o banco de dados
     * Testa se é possível conectar com as credenciais configuradas
     * 
     * @throws RuntimeException Se não conseguir conectar ao banco
     */
    public static void validarConexaoBancoDados() {
        logger.info("Validando conexão com o banco de dados...");

        final String url = obterUrlBancoDados();
        final String usuario = obterUsuarioBancoDados();
        final String senha = obterSenhaBancoDados();

        if (url == null || url.trim().isEmpty()) {
            logger.error("URL do banco de dados não configurada");
            throw new RuntimeException("Configuração inválida: URL do banco de dados não pode estar vazia");
        }

        if (usuario == null || usuario.trim().isEmpty()) {
            logger.error("Usuário do banco de dados não configurado");
            throw new RuntimeException("Configuração inválida: Usuário do banco de dados não pode estar vazio");
        }

        if (senha == null || senha.trim().isEmpty()) {
            logger.error("Senha do banco de dados não configurada");
            throw new RuntimeException("Configuração inválida: Senha do banco de dados não pode estar vazia");
        }

        try (Connection conexao = GerenciadorConexao.obterConexao()) {
            // Testa se a conexão é válida
            if (conexao.isValid(5)) { // timeout de 5 segundos
                logger.info("✓ Conexão com banco de dados validada com sucesso (via pool HikariCP)");
            } else {
                logger.error("Conexão com banco de dados inválida (via pool HikariCP)");
                throw new RuntimeException("Falha na validação: Conexão com banco de dados inválida");
            }
        } catch (final SQLException e) {
            logger.error("Erro ao conectar com o banco de dados: {}", e.getMessage());

            // Mensagens de erro mais específicas baseadas no código de erro
            String mensagemErro = "Erro de conexão com banco de dados: ";
            if (e.getMessage().contains("Login failed")) {
                mensagemErro += "Credenciais inválidas (usuário ou senha incorretos)";
            } else if (e.getMessage().contains("Cannot open database")) {
                mensagemErro += "Banco de dados não encontrado ou inacessível";
            } else if (e.getMessage().contains("The TCP/IP connection")) {
                mensagemErro += "Servidor de banco de dados inacessível (verifique URL e conectividade)";
            } else {
                mensagemErro += e.getMessage();
            }

            throw new RuntimeException(mensagemErro, e);
        }
    }
    
    /**
     * Valida se as tabelas essenciais existem no banco de dados.
     * Verifica: log_extracoes, page_audit, dim_usuarios
     * 
     * @throws IllegalStateException Se alguma tabela essencial não existir
     */
    public static void validarTabelasEssenciais() {
        logger.info("🔍 Validando existência de tabelas essenciais no banco de dados...");
        
        final String[] tabelasEssenciais = {
            "log_extracoes",
            "page_audit",
            "dim_usuarios"
        };
        
        final StringBuilder tabelasFaltando = new StringBuilder();
        
        try (Connection conexao = GerenciadorConexao.obterConexao()) {
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
                    "❌ ERRO CRÍTICO: As seguintes tabelas não existem no banco de dados: %s. " +
                    "Execute 'database/executar_database.bat' antes de rodar a aplicação. " +
                    "Veja database/README.md para instruções.",
                    tabelasFaltando.toString()
                );
                logger.error(mensagem);
                throw new IllegalStateException(mensagem);
            }
            
            logger.info("✅ Todas as tabelas essenciais existem no banco de dados");
            
        } catch (final SQLException e) {
            logger.error("Erro ao validar tabelas essenciais: {}", e.getMessage());
            throw new RuntimeException("Falha ao validar tabelas essenciais", e);
        }
    }
    
    /**
     * Verifica se uma tabela existe no banco de dados.
     * 
     * @param conexao Conexão com o banco de dados
     * @param nomeTabela Nome da tabela a verificar
     * @return true se a tabela existe, false caso contrário
     * @throws SQLException Se houver erro ao consultar o banco
     */
    private static boolean tabelaExiste(final Connection conexao, final String nomeTabela) throws SQLException {
        final String sql = """
            SELECT COUNT(*) 
            FROM INFORMATION_SCHEMA.TABLES 
            WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = ?
            """;
        
        try (java.sql.PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, nomeTabela);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Obtém uma configuração obrigatória exclusivamente de variáveis de ambiente.
     * Implementa lógica de fail-fast para dados sensíveis.
     * 
     * @param nomeVariavelAmbiente Nome da variável de ambiente obrigatória
     * @return Valor da variável de ambiente
     * @throws IllegalStateException Se a variável de ambiente não existir ou estiver vazia
     */
    private static String obterConfiguracaoObrigatoria(final String nomeVariavelAmbiente) {
        final String valor = System.getenv(nomeVariavelAmbiente);
        
        if (valor == null || valor.trim().isEmpty()) {
            final String mensagem = String.format(
                "Variável de ambiente obrigatória '%s' não encontrada ou está vazia. " +
                "Configure esta variável de ambiente antes de executar a aplicação.",
                nomeVariavelAmbiente
            );
            logger.error(mensagem);
            throw new IllegalStateException(mensagem);
        }
        
        logger.debug("Configuração sensível '{}' obtida da variável de ambiente", nomeVariavelAmbiente);
        return valor;
    }

    /**
     * Obtém uma configuração priorizando variáveis de ambiente sobre o arquivo
     * config.properties. Para configurações não-sensíveis.
     * 
     * @param nomeVariavelAmbiente Nome da variável de ambiente
     * @param nomeChaveProperties  Nome da chave no arquivo config.properties
     * @return Valor da configuração (variável de ambiente ou fallback para
     *         properties)
     */
    private static String obterConfiguracao(final String nomeVariavelAmbiente, final String nomeChaveProperties) {
        // Tenta primeiro obter da variável de ambiente
        final String valorAmbiente = System.getenv(nomeVariavelAmbiente);
        if (valorAmbiente != null && !valorAmbiente.trim().isEmpty()) {
            logger.debug("Configuração '{}' obtida da variável de ambiente", nomeVariavelAmbiente);
            return valorAmbiente;
        }

        // Fallback para o arquivo config.properties
        final Properties props = carregarPropriedades();
        final String valorProperties = props.getProperty(nomeChaveProperties);
        if (valorProperties == null) {
            logger.warn(
                    "Configuração '{}' não encontrada nem em variável de ambiente '{}' nem no arquivo de configuração '{}'",
                    nomeChaveProperties, nomeVariavelAmbiente, nomeChaveProperties);
        } else {
            logger.debug("Configuração '{}' obtida do arquivo config.properties", nomeChaveProperties);
        }
        return valorProperties;
    }

    /**
     * Obtém uma propriedade específica do arquivo de configuração
     * 
     * @param chave Nome da propriedade
     * @return Valor da propriedade
     */
    public static String obterPropriedade(final String chave) {
        final Properties props = carregarPropriedades();
        final String valor = props.getProperty(chave);
        if (valor == null) {
            logger.warn("Propriedade '{}' não encontrada no arquivo de configuração", chave);
        }
        return valor;
    }

    /**
     * Obtém a URL base da API
     * 
     * @return URL base da API
     */
    public static String obterUrlBaseApi() {
        return obterConfiguracao("API_BASEURL", "api.baseurl");
    }

    /**
     * Obtém o token de autenticação da API REST
     * 
     * @return Token de autenticação da API REST
     * @throws IllegalStateException Se a variável de ambiente API_REST_TOKEN não estiver configurada
     */
    public static String obterTokenApiRest() {
        return obterConfiguracaoObrigatoria("API_REST_TOKEN");
    }

    /**
     * Obtém o token de autenticação da API GraphQL
     * 
     * @return Token de autenticação da API GraphQL
     * @throws IllegalStateException Se a variável de ambiente API_GRAPHQL_TOKEN não estiver configurada
     */
    public static String obterTokenApiGraphQL() {
        return obterConfiguracaoObrigatoria("API_GRAPHQL_TOKEN");
    }

    /**
     * Obtém o endpoint da API GraphQL.
     * 
     * @return Endpoint da API GraphQL
     */
    public static String obterEndpointGraphQL() {
        return obterConfiguracao("API_GRAPHQL_ENDPOINT", "api.graphql.endpoint");
    }

    /**
     * Obtém o token da API Data Export.
     * 
     * @return Token da API Data Export
     * @throws IllegalStateException Se a variável de ambiente API_DATAEXPORT_TOKEN não estiver configurada
     */
    public static String obterTokenApiDataExport() {
        return obterConfiguracaoObrigatoria("API_DATAEXPORT_TOKEN");
    }

    /**
     * Obtém a URL de conexão com o banco de dados
     * 
     * @return URL de conexão com o banco
     * @throws IllegalStateException Se a variável de ambiente DB_URL não estiver configurada
     */
    public static String obterUrlBancoDados() {
        return obterConfiguracaoObrigatoria("DB_URL");
    }

    /**
     * Obtém o usuário do banco de dados
     * 
     * @return Usuário do banco
     * @throws IllegalStateException Se a variável de ambiente DB_USER não estiver configurada
     */
    public static String obterUsuarioBancoDados() {
        return obterConfiguracaoObrigatoria("DB_USER");
    }

    /**
     * Obtém a senha do banco de dados
     * 
     * @return Senha do banco
     * @throws IllegalStateException Se a variável de ambiente DB_PASSWORD não estiver configurada
     */
    public static String obterSenhaBancoDados() {
        return obterConfiguracaoObrigatoria("DB_PASSWORD");
    }
    
    /**
     * Obtém o nome do banco de dados alvo.
     * Prioriza variável de ambiente DB_NAME; fallback para config.properties (db.name).
     */
    public static String obterNomeBancoDados() {
        final String valor = obterConfiguracao("DB_NAME", "db.name");
        return valor;
    }

    /**
     * Obtém o tempo de espaçamento padrão (throttling) entre requisições em
     * milissegundos.
     * 
     * @return O tempo de throttling em ms.
     */
    public static long obterThrottlingPadrao() {
        final String valor = obterConfiguracao("API_THROTTLING_PADRAO_MS", "api.throttling.padrao_ms");
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException | NullPointerException e) {
            logger.warn(
                    "Propriedade 'api.throttling.padrao_ms' não encontrada ou inválida. Usando valor padrão: 2000ms");
            return 2000L; // Valor padrão de 2 segundos
        }
    }

    /**
     * Obtém o número máximo de tentativas para a lógica de retry.
     * 
     * @return O número máximo de tentativas.
     */
    public static int obterMaxTentativasRetry() {
        final String valor = obterConfiguracao("API_RETRY_MAX_TENTATIVAS", "api.retry.max_tentativas");
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException | NullPointerException e) {
            logger.warn("Propriedade 'api.retry.max_tentativas' não encontrada ou inválida. Usando valor padrão: 5");
            return 5; // Valor padrão
        }
    }

    /**
     * Obtém o tempo de espera base (em milissegundos) para a lógica de retry.
     * 
     * @return O tempo de delay base em ms.
     */
    public static long obterDelayBaseRetry() {
        final String valor = obterConfiguracao("API_RETRY_DELAY_BASE_MS", "api.retry.delay_base_ms");
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException | NullPointerException e) {
            logger.warn(
                    "Propriedade 'api.retry.delay_base_ms' não encontrada ou inválida. Usando valor padrão: 2000ms");
            return 2000L; // Valor padrão de 2 segundos
        }
    }

    /**
     * Obtém o multiplicador para a estratégia de backoff exponencial.
     * 
     * @return O multiplicador.
     */
    public static double obterMultiplicadorRetry() {
        final String valor = obterConfiguracao("API_RETRY_MULTIPLICADOR", "api.retry.multiplicador");
        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException | NullPointerException e) {
            logger.warn("Propriedade 'api.retry.multiplicador' não encontrada ou inválida. Usando valor padrão: 2.0");
            return 2.0; // Valor padrão
        }
    }

    /**
     * Obtém o timeout para requisições da API REST em Duration.
     * 
     * @return Duration com o timeout configurado (padrão: 120 segundos)
     */
    public static java.time.Duration obterTimeoutApiRest() {
        final String valor = obterConfiguracao("API_REST_TIMEOUT_SECONDS", "api.rest.timeout.seconds");
        try {
            final long segundos = Long.parseLong(valor);
            return java.time.Duration.ofSeconds(segundos);
        } catch (NumberFormatException | NullPointerException e) {
            logger.warn("Propriedade 'api.rest.timeout.seconds' não encontrada ou inválida. Usando valor padrão: 120 segundos");
            return java.time.Duration.ofSeconds(120L); // Valor padrão de 120 segundos
        }
    }

    /**
     * Obtém o ID da corporação para filtros GraphQL
     * 
     * @return ID da corporação
     */
    public static String obterCorporationId() {
        return obterConfiguracao("API_CORPORATION_ID", "api.corporation.id");
    }

    /**
     * Obtém o intervalo mínimo de throttling (padrão: 2200ms)
     * Usado para evitar erros HTTP 429 (Rate Limit)
     * 
     * @return O intervalo mínimo de throttling em ms
     */
    public static long obterThrottlingMinimo() {
        final String valor = obterConfiguracao("API_THROTTLING_MINIMO_MS", "api.throttling.minimo_ms");
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException | NullPointerException e) {
            logger.warn("Propriedade 'api.throttling.minimo_ms' não encontrada ou inválida. Usando valor padrão: 2200ms");
            return 2200L; // Valor padrão de 2.2 segundos (10% de margem sobre 2s)
        }
    }

    /**
     * Obtém o limite máximo de páginas por execução para API REST
     * 
     * @return Limite máximo de páginas (padrão: 500)
     */
    public static int obterLimitePaginasApiRest() {
        final String valor = obterConfiguracao("API_REST_MAX_PAGINAS", "api.rest.max.paginas");
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException | NullPointerException e) {
            logger.warn("Propriedade 'api.rest.max.paginas' não encontrada ou inválida. Usando valor padrão: 500");
            return 500; // Valor padrão aumentado de 100 para 500
        }
    }

    /**
     * Obtém o limite máximo de páginas por execução para API GraphQL
     * 
     * @return Limite máximo de páginas (padrão: 2000)
     */
    public static int obterLimitePaginasApiGraphQL() {
        final String valor = obterConfiguracao("API_GRAPHQL_MAX_PAGINAS", "api.graphql.max.paginas");
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException | NullPointerException e) {
            logger.warn("Propriedade 'api.graphql.max.paginas' não encontrada ou inválida. Usando valor padrão: 2000");
            return 2000; // Valor padrão aumentado de 1000 para 2000
        }
    }

    public static int obterLimitePaginasFaturasGraphQL() {
        final String valor = obterConfiguracao("API_GRAPHQL_FATURAS_MAX_PAGINAS", "api.graphql.faturas.max_paginas");
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException | NullPointerException e) {
            logger.warn("Propriedade 'api.graphql.faturas.max_paginas' não encontrada ou inválida. Usando valor padrão: 200");
            return 200;
        }
    }

    public static int obterDiasJanelaFaturasGraphQL() {
        final String valor = obterConfiguracao("API_GRAPHQL_FATURAS_DIAS_JANELA", "api.graphql.faturas.dias_janela");
        try {
            final int dias = Integer.parseInt(valor);
            return dias <= 0 ? 2 : dias;
        } catch (NumberFormatException | NullPointerException e) {
            logger.warn("Propriedade 'api.graphql.faturas.dias_janela' não encontrada ou inválida. Usando valor padrão: 2");
            return 2;
        }
    }

    /**
     * Obtém o limite máximo de páginas por execução para API DataExport
     * 
     * @return Limite máximo de páginas (padrão: 500)
     */
    public static int obterLimitePaginasApiDataExport() {
        final String valor = obterConfiguracao("API_DATAEXPORT_MAX_PAGINAS", "api.dataexport.max.paginas");
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException | NullPointerException e) {
            logger.warn("Propriedade 'api.dataexport.max.paginas' não encontrada ou inválida. Usando valor padrão: 500");
            return 500; // Valor padrão aumentado de 100 para 500
        }
    }

    /**
     * Obtém o limite máximo de REGISTROS por execução para API GraphQL.
     * PROBLEMA #7 CORRIGIDO: Valor agora configurável em vez de hardcoded.
     * 
     * @return Limite máximo de registros (padrão: 50000)
     */
    public static int obterMaxRegistrosGraphQL() {
        final String valor = obterConfiguracao("API_GRAPHQL_MAX_REGISTROS", "api.graphql.max.registros.execucao");
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException | NullPointerException e) {
            logger.debug("Propriedade 'api.graphql.max.registros.execucao' não encontrada. Usando padrão: 50000");
            return 50000;
        }
    }

    /**
     * Obtém o limite máximo de REGISTROS por execução para API DataExport.
     * PROBLEMA #7 CORRIGIDO: Valor agora configurável em vez de hardcoded.
     * 
     * @return Limite máximo de registros (padrão: 10000)
     */
    public static int obterMaxRegistrosDataExport() {
        final String valor = obterConfiguracao("API_DATAEXPORT_MAX_REGISTROS", "api.dataexport.max.registros.execucao");
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException | NullPointerException e) {
            logger.debug("Propriedade 'api.dataexport.max.registros.execucao' não encontrada. Usando padrão: 10000");
            return 10000;
        }
    }

    // ========== CONFIGURAÇÕES DE BANCO DE DADOS ==========

    /**
     * Obtém o tamanho do batch para commits no banco de dados.
     * Controla quantos registros são processados antes de cada commit.
     * 
     * @return Tamanho do batch (padrão: 100)
     */
    public static int obterBatchSize() {
        final String valor = obterConfiguracao("DB_BATCH_SIZE", "db.batch.size");
        try {
            final int size = Integer.parseInt(valor);
            return size > 0 ? size : 100;
        } catch (NumberFormatException | NullPointerException e) {
            return 100;
        }
    }

    /**
     * Determina se a extração deve continuar após erro em um registro.
     * Se false, para na primeira falha. Se true, continua e loga os erros.
     * 
     * @return true para continuar após erros (padrão: true)
     */
    public static boolean isContinuarAposErro() {
        final String valor = obterConfiguracao("DB_CONTINUE_ON_ERROR", "db.continue.on.error");
        if (valor == null || valor.isEmpty()) {
            return true; // Padrão: continuar
        }
        return Boolean.parseBoolean(valor);
    }

    /**
     * Obtém o delay entre extrações de entidades em milissegundos.
     * Usado para evitar sobrecarga nas APIs.
     * 
     * @return Delay em ms (padrão: 2000)
     */
    public static long obterDelayEntreExtracoes() {
        final String valor = obterConfiguracao("EXTRACAO_DELAY_MS", "extracao.delay.ms");
        try {
            final long delay = Long.parseLong(valor);
            return delay > 0 ? delay : 2000L;
        } catch (NumberFormatException | NullPointerException e) {
            return 2000L;
        }
    }

    /**
     * Obtém o timeout de validação de conexão em segundos.
     * 
     * @return Timeout em segundos (padrão: 5)
     */
    public static int obterTimeoutValidacaoConexao() {
        final String valor = obterConfiguracao("DB_VALIDATION_TIMEOUT", "db.validation.timeout");
        try {
            final int timeout = Integer.parseInt(valor);
            return timeout > 0 ? timeout : 5;
        } catch (NumberFormatException | NullPointerException e) {
            return 5;
        }
    }

    // ========== FASE 4: CONFIGURAÇÕES DE ENRIQUECIMENTO DE FATURAS ==========

    /**
     * Obtém o número de threads para processamento paralelo de resultados do enriquecimento.
     * IMPORTANTE: Essas threads são para processamento de dados (parsing, mapeamento, salvamento),
     * NÃO para requisições HTTP. As requisições HTTP continuam sequenciais com throttling global de 2s.
     * 
     * @return Número de threads (padrão: 5)
     */
    public static int obterThreadsProcessamentoFaturas() {
        final String valor = obterConfiguracao("API_ENRIQUECIMENTO_FATURAS_THREADS", "api.enriquecimento.faturas.threads");
        try {
            final int threads = Integer.parseInt(valor);
            return threads > 0 ? threads : 5;
        } catch (NumberFormatException | NullPointerException e) {
            logger.debug("Propriedade 'api.enriquecimento.faturas.threads' não encontrada. Usando padrão: 5");
            return 5;
        }
    }

    /**
     * Obtém o limite de erros consecutivos antes de aumentar delay.
     * 
     * @return Limite de erros (padrão: 10)
     */
    public static int obterLimiteErrosConsecutivos() {
        final String valor = obterConfiguracao("API_ENRIQUECIMENTO_ERROS_LIMITE", "api.enriquecimento.erros_consecutivos_limite");
        try {
            final int limite = Integer.parseInt(valor);
            return limite > 0 ? limite : 10;
        } catch (NumberFormatException | NullPointerException e) {
            return 10;
        }
    }

    /**
     * Obtém o multiplicador de delay quando há muitos erros consecutivos.
     * 
     * @return Multiplicador (padrão: 2.0)
     */
    public static double obterMultiplicadorDelayErros() {
        final String valor = obterConfiguracao("API_ENRIQUECIMENTO_DELAY_MULTIPLIER", "api.enriquecimento.delay_multiplier_erros");
        try {
            final double multiplicador = Double.parseDouble(valor);
            return multiplicador > 1.0 ? multiplicador : 2.0;
        } catch (NumberFormatException | NullPointerException e) {
            return 2.0;
        }
    }

    /**
     * Obtém o intervalo (em número de faturas) para log de progresso.
     * 
     * @return Intervalo (padrão: 100)
     */
    public static int obterIntervaloLogProgressoEnriquecimento() {
        final String valor = obterConfiguracao("API_ENRIQUECIMENTO_INTERVALO_LOG", "api.enriquecimento.intervalo_log_progresso");
        try {
            final int intervalo = Integer.parseInt(valor);
            return intervalo > 0 ? intervalo : 100;
        } catch (NumberFormatException | NullPointerException e) {
            return 100;
        }
    }

    /**
     * Obtém o intervalo (em segundos) para heartbeat durante enriquecimento.
     * 
     * @return Intervalo em segundos (padrão: 10)
     */
    public static int obterHeartbeatSegundos() {
        final String valor = obterConfiguracao("API_ENRIQUECIMENTO_HEARTBEAT", "api.enriquecimento.heartbeat_segundos");
        try {
            final int segundos = Integer.parseInt(valor);
            return segundos > 0 ? segundos : 10;
        } catch (NumberFormatException | NullPointerException e) {
            return 10;
        }
    }
}
