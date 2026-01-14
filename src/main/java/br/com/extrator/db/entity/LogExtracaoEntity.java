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
        
        StatusExtracao(final String valor) {
            this.valor = valor;
        }
        
        public String getValor() {
            return valor;
        }
        
        public static StatusExtracao fromString(final String valor) {
            // Mapear "INCOMPLETO" para "INCOMPLETO_LIMITE" para compatibilidade
            if ("INCOMPLETO".equals(valor)) {
                return INCOMPLETO_LIMITE;
            }
            
            for (final StatusExtracao status : StatusExtracao.values()) {
                if (status.valor.equals(valor)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Status inválido: " + valor);
        }
    }
    
    // Construtores
    public LogExtracaoEntity() {}
    
    public LogExtracaoEntity(final String entidade, final LocalDateTime timestampInicio, final LocalDateTime timestampFim,
                           final StatusExtracao statusFinal, final Integer registrosExtraidos, final Integer paginasProcessadas,
                           final String mensagem) {
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
    public LogExtracaoEntity(final String entidade, final LocalDateTime timestampInicio, final LocalDateTime timestampFim,
                           final String statusFinal, final Integer registrosExtraidos, final Integer paginasProcessadas,
                           final String mensagem) {
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
    
    public void setId(final Long id) {
        this.id = id;
    }
    
    public String getEntidade() {
        return entidade;
    }
    
    public void setEntidade(final String entidade) {
        this.entidade = entidade;
    }
    
    public LocalDateTime getTimestampInicio() {
        return timestampInicio;
    }
    
    public void setTimestampInicio(final LocalDateTime timestampInicio) {
        this.timestampInicio = timestampInicio;
    }
    
    public LocalDateTime getTimestampFim() {
        return timestampFim;
    }
    
    public void setTimestampFim(final LocalDateTime timestampFim) {
        this.timestampFim = timestampFim;
    }
    
    public StatusExtracao getStatusFinal() {
        return statusFinal;
    }
    
    public void setStatusFinal(final StatusExtracao statusFinal) {
        this.statusFinal = statusFinal;
    }
    
    public Integer getRegistrosExtraidos() {
        return registrosExtraidos;
    }
    
    public void setRegistrosExtraidos(final Integer registrosExtraidos) {
        this.registrosExtraidos = registrosExtraidos;
    }
    
    public Integer getPaginasProcessadas() {
        return paginasProcessadas;
    }
    
    public void setPaginasProcessadas(final Integer paginasProcessadas) {
        this.paginasProcessadas = paginasProcessadas;
    }
    
    public String getMensagem() {
        return mensagem;
    }
    
    public void setMensagem(final String mensagem) {
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