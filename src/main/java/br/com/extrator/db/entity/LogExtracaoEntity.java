package br.com.extrator.db.entity;

import java.time.LocalDateTime;

/**
 * Entidade para controle de status das extrações de dados
 */
public class LogExtracaoEntity {
    
    private Long id;
    private String entidade;
    private LocalDateTime timestampInicio;
    private LocalDateTime timestampFim;
    private StatusExtracao statusFinal;
    private Integer registrosExtraidos;
    private Integer paginasProcessadas;
    private String mensagem;
    
    public enum StatusExtracao {
        COMPLETO("COMPLETO"),
        INCOMPLETO_LIMITE("INCOMPLETO_LIMITE"),
        ERRO_API("ERRO_API");
        
        private final String valor;
        
        StatusExtracao(String valor) {
            this.valor = valor;
        }
        
        public String getValor() {
            return valor;
        }
        
        public static StatusExtracao fromString(String valor) {
            for (StatusExtracao status : StatusExtracao.values()) {
                if (status.valor.equals(valor)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Status inválido: " + valor);
        }
    }
    
    // Construtores
    public LogExtracaoEntity() {}
    
    public LogExtracaoEntity(String entidade, LocalDateTime timestampInicio, LocalDateTime timestampFim,
                           StatusExtracao statusFinal, Integer registrosExtraidos, Integer paginasProcessadas,
                           String mensagem) {
        this.entidade = entidade;
        this.timestampInicio = timestampInicio;
        this.timestampFim = timestampFim;
        this.statusFinal = statusFinal;
        this.registrosExtraidos = registrosExtraidos;
        this.paginasProcessadas = paginasProcessadas;
        this.mensagem = mensagem;
    }
    
    /**
     * Construtor que aceita String para status (converte automaticamente para enum)
     */
    public LogExtracaoEntity(String entidade, LocalDateTime timestampInicio, LocalDateTime timestampFim,
                           String statusFinal, Integer registrosExtraidos, Integer paginasProcessadas,
                           String mensagem) {
        this.entidade = entidade;
        this.timestampInicio = timestampInicio;
        this.timestampFim = timestampFim;
        this.statusFinal = StatusExtracao.fromString(statusFinal);
        this.registrosExtraidos = registrosExtraidos;
        this.paginasProcessadas = paginasProcessadas;
        this.mensagem = mensagem;
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEntidade() {
        return entidade;
    }
    
    public void setEntidade(String entidade) {
        this.entidade = entidade;
    }
    
    public LocalDateTime getTimestampInicio() {
        return timestampInicio;
    }
    
    public void setTimestampInicio(LocalDateTime timestampInicio) {
        this.timestampInicio = timestampInicio;
    }
    
    public LocalDateTime getTimestampFim() {
        return timestampFim;
    }
    
    public void setTimestampFim(LocalDateTime timestampFim) {
        this.timestampFim = timestampFim;
    }
    
    public StatusExtracao getStatusFinal() {
        return statusFinal;
    }
    
    public void setStatusFinal(StatusExtracao statusFinal) {
        this.statusFinal = statusFinal;
    }
    
    public Integer getRegistrosExtraidos() {
        return registrosExtraidos;
    }
    
    public void setRegistrosExtraidos(Integer registrosExtraidos) {
        this.registrosExtraidos = registrosExtraidos;
    }
    
    public Integer getPaginasProcessadas() {
        return paginasProcessadas;
    }
    
    public void setPaginasProcessadas(Integer paginasProcessadas) {
        this.paginasProcessadas = paginasProcessadas;
    }
    
    public String getMensagem() {
        return mensagem;
    }
    
    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
    
    @Override
    public String toString() {
        return "LogExtracaoEntity{" +
                "id=" + id +
                ", entidade='" + entidade + '\'' +
                ", timestampInicio=" + timestampInicio +
                ", timestampFim=" + timestampFim +
                ", statusFinal=" + statusFinal +
                ", registrosExtraidos=" + registrosExtraidos +
                ", paginasProcessadas=" + paginasProcessadas +
                ", mensagem='" + mensagem + '\'' +
                '}';
    }
}