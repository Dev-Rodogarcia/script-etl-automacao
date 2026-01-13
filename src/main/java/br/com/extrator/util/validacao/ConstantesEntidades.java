package br.com.extrator.util.validacao;

import br.com.extrator.util.configuracao.CarregadorConfig;

/**
 * Constantes centralizadas para nomes de entidades utilizadas no sistema.
 * Evita duplicação de strings "magic" espalhadas pelo código.
 * 
 * @author Sistema de Extração ESL Cloud
 * @version 1.0
 */
public final class ConstantesEntidades {
    
    // ========== ENTIDADES GRAPHQL ==========
    public static final String COLETAS = "coletas";
    public static final String FRETES = "fretes";
    public static final String FATURAS_GRAPHQL = "faturas_graphql";
    
    // ========== ENTIDADES DATA EXPORT ==========
    public static final String MANIFESTOS = "manifestos";
    public static final String COTACOES = "cotacoes";
    public static final String LOCALIZACAO_CARGAS = "localizacao_cargas";
    public static final String CONTAS_A_PAGAR = "contas_a_pagar";
    public static final String FATURAS_POR_CLIENTE = "faturas_por_cliente";
    
    // ========== ALIASES PARA COMPATIBILIDADE ==========
    /** Aliases para input do usuário (múltiplas formas aceitas) */
    public static final String[] ALIASES_COTACOES = {"cotacoes", "cotacao"};
    public static final String[] ALIASES_LOCALIZACAO = {"localizacao_carga", "localizacao_de_carga", "localizacao-carga", "localizacao de carga"};
    public static final String[] ALIASES_CONTAS_PAGAR = {"contas_a_pagar", "contasapagar", "contas a pagar", "contas-a-pagar"};
    public static final String[] ALIASES_FATURAS_CLIENTE = {"faturas_por_cliente", "faturasporcliente", "faturas por cliente", "faturas-por-cliente"};
    public static final String[] ALIASES_FATURAS_GRAPHQL = {"faturas_graphql", "faturas"};
    
    // ========== STATUS DE EXTRAÇÃO ==========
    public static final String STATUS_COMPLETO = "COMPLETO";
    public static final String STATUS_INCOMPLETO = "INCOMPLETO";
    public static final String STATUS_ERRO_API = "ERRO_API";
    
    // ========== DELAY ENTRE EXTRAÇÕES (configurável) ==========
    /** 
     * Obtém o delay padrão entre extrações de entidades.
     * Utiliza a configuração de extracao.delay.ms do CarregadorConfig.
     * @return delay em milissegundos
     */
    public static long obterDelayEntreExtracoes() {
        return CarregadorConfig.obterDelayEntreExtracoes();
    }
    
    /**
     * Aplica delay entre extrações de entidades.
     * Usa valor configurável em vez de hardcoded.
     * @throws InterruptedException se a thread for interrompida
     */
    public static void aplicarDelayEntreExtracoes() throws InterruptedException {
        Thread.sleep(obterDelayEntreExtracoes());
    }
    
    private ConstantesEntidades() {
        // Impede instanciação
    }
}

