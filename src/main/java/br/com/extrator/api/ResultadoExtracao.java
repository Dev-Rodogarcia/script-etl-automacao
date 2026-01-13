package br.com.extrator.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe que encapsula o resultado de uma extração de dados,
 * incluindo informações sobre se a extração foi completa ou interrompida.
 */
public class ResultadoExtracao<T> {
    
    private final List<T> dados;
    private final boolean completo;
    private final String motivoInterrupcao;
    private final int paginasProcessadas;
    private final int registrosExtraidos;
    
    public enum MotivoInterrupcao {
        LIMITE_PAGINAS("LIMITE_PAGINAS"),
        LIMITE_REGISTROS("LIMITE_REGISTROS"),
        LOOP_DETECTADO("LOOP_DETECTADO"),
        ERRO_API("ERRO_API"),
        CIRCUIT_BREAKER("CIRCUIT_BREAKER"),
        PAGINA_VAZIA("PAGINA_VAZIA");
        
        private final String codigo;
        
        MotivoInterrupcao(String codigo) {
            this.codigo = codigo;
        }
        
        public String getCodigo() {
            return codigo;
        }
    }
    
    // Construtor para extração completa
    public ResultadoExtracao(List<T> dados, int paginasProcessadas, int registrosExtraidos) {
        this.dados = dados != null ? dados : new ArrayList<>();
        this.completo = true;
        this.motivoInterrupcao = null;
        this.paginasProcessadas = paginasProcessadas;
        this.registrosExtraidos = registrosExtraidos;
    }
    
    // Construtor para extração incompleta
    public ResultadoExtracao(List<T> dados, MotivoInterrupcao motivo, int paginasProcessadas, int registrosExtraidos) {
        this.dados = dados != null ? dados : new ArrayList<>();
        this.completo = false;
        this.motivoInterrupcao = motivo.getCodigo();
        this.paginasProcessadas = paginasProcessadas;
        this.registrosExtraidos = registrosExtraidos;
    }
    
    // Construtor para extração incompleta com motivo customizado
    public ResultadoExtracao(List<T> dados, String motivoCustomizado, int paginasProcessadas, int registrosExtraidos) {
        this.dados = dados != null ? dados : new ArrayList<>();
        this.completo = false;
        this.motivoInterrupcao = motivoCustomizado;
        this.paginasProcessadas = paginasProcessadas;
        this.registrosExtraidos = registrosExtraidos;
    }
    
    // Getters
    public List<T> getDados() {
        return dados;
    }
    
    public boolean isCompleto() {
        return completo;
    }
    
    public String getMotivoInterrupcao() {
        return motivoInterrupcao;
    }
    
    public int getPaginasProcessadas() {
        return paginasProcessadas;
    }
    
    public int getRegistrosExtraidos() {
        return registrosExtraidos;
    }
    
    /**
     * Cria um resultado de extração completa
     */
    public static <T> ResultadoExtracao<T> completo(List<T> dados, int paginasProcessadas, int registrosExtraidos) {
        return new ResultadoExtracao<>(dados, paginasProcessadas, registrosExtraidos);
    }
    
    /**
     * Cria um resultado de extração incompleta
     */
    public static <T> ResultadoExtracao<T> incompleto(List<T> dados, MotivoInterrupcao motivo, int paginasProcessadas, int registrosExtraidos) {
        return new ResultadoExtracao<>(dados, motivo, paginasProcessadas, registrosExtraidos);
    }
    
    /**
     * Cria um resultado de extração incompleta com motivo customizado
     */
    public static <T> ResultadoExtracao<T> incompleto(List<T> dados, String motivoCustomizado, int paginasProcessadas, int registrosExtraidos) {
        return new ResultadoExtracao<>(dados, motivoCustomizado, paginasProcessadas, registrosExtraidos);
    }
    
    @Override
    public String toString() {
        return "ResultadoExtracao{" +
                "registrosExtraidos=" + registrosExtraidos +
                ", paginasProcessadas=" + paginasProcessadas +
                ", completo=" + completo +
                (motivoInterrupcao != null ? ", motivoInterrupcao='" + motivoInterrupcao + '\'' : "") +
                '}';
    }
}