package br.com.extrator.util.validacao;

import br.com.extrator.util.ThreadUtil;
import br.com.extrator.util.configuracao.CarregadorConfig;

/**
 * Constantes centralizadas para nomes de entidades utilizadas no sistema.
 * Evita duplicaÃ§Ã£o de strings "magic" espalhadas pelo cÃ³digo.
 * 
 * @author Sistema de ExtraÃ§Ã£o ESL Cloud
 * @version 1.0
 */
public final class ConstantesEntidades {
    
    // ========== ENTIDADES GRAPHQL ==========
    public static final String COLETAS = "coletas";
    public static final String FRETES = "fretes";
    public static final String FATURAS_GRAPHQL = "faturas_graphql";
    public static final String USUARIOS_SISTEMA = "usuarios_sistema";
    
    // ========== ENTIDADES DATA EXPORT ==========
    public static final String MANIFESTOS = "manifestos";
    public static final String COTACOES = "cotacoes";
    public static final String LOCALIZACAO_CARGAS = "localizacao_cargas";
    public static final String CONTAS_A_PAGAR = "contas_a_pagar";
    public static final String FATURAS_POR_CLIENTE = "faturas_por_cliente";
    
    // ========== ALIASES PARA COMPATIBILIDADE ==========
    /** Aliases para input do usuÃ¡rio (mÃºltiplas formas aceitas) */
    public static final String[] ALIASES_COTACOES = {"cotacoes", "cotacao"};
    public static final String[] ALIASES_LOCALIZACAO = {"localizacao_carga", "localizacao_de_carga", "localizacao-carga", "localizacao de carga"};
    public static final String[] ALIASES_CONTAS_PAGAR = {"contas_a_pagar", "contasapagar", "contas a pagar", "contas-a-pagar"};
    public static final String[] ALIASES_FATURAS_CLIENTE = {"faturas_por_cliente", "faturasporcliente", "faturas por cliente", "faturas-por-cliente"};
    public static final String[] ALIASES_FATURAS_GRAPHQL = {"faturas_graphql", "faturas"};
    
    // ========== STATUS DE EXTRAÃ‡ÃƒO ==========
    public static final String STATUS_COMPLETO = "COMPLETO";
    /**
     * Status legado genÃ©rico. Mantido para compatibilidade com histÃ³ricos antigos.
     */
    public static final String STATUS_INCOMPLETO = "INCOMPLETO";
    /**
     * ExtraÃ§Ã£o interrompida por proteÃ§Ã£o de paginaÃ§Ã£o/volume/circuit breaker.
     */
    public static final String STATUS_INCOMPLETO_LIMITE = "INCOMPLETO_LIMITE";
    /**
     * Dados invÃ¡lidos recebidos da origem (campos crÃ­ticos nulos/ilegais).
     */
    public static final String STATUS_INCOMPLETO_DADOS = "INCOMPLETO_DADOS";
    /**
     * DivergÃªncia entre volume Ãºnico esperado e volume efetivamente persistido.
     */
    public static final String STATUS_INCOMPLETO_DB = "INCOMPLETO_DB";
    public static final String STATUS_ERRO_API = "ERRO_API";
    
    // ========== DELAY ENTRE EXTRAÃ‡Ã•ES (configurÃ¡vel) ==========
    /** 
     * ObtÃ©m o delay padrÃ£o entre extraÃ§Ãµes de entidades.
     * Utiliza a configuraÃ§Ã£o de extracao.delay.ms do CarregadorConfig.
     * @return delay em milissegundos
     */
    public static long obterDelayEntreExtracoes() {
        return CarregadorConfig.obterDelayEntreExtracoes();
    }
    
    /**
     * Aplica delay entre extraÃ§Ãµes de entidades.
     * Usa valor configurÃ¡vel em vez de hardcoded.
     * @throws InterruptedException se a thread for interrompida
     */
    public static void aplicarDelayEntreExtracoes() throws InterruptedException {
        ThreadUtil.aguardar(obterDelayEntreExtracoes());
    }
    
    private ConstantesEntidades() {
        // Impede instanciaÃ§Ã£o
    }
}
