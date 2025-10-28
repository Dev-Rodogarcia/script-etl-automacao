package br.com.extrator.auditoria;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe que representa o resultado da validação de uma entidade específica.
 * Contém informações sobre a completude dos dados, estatísticas e observações.
 */
public class ResultadoValidacaoEntidade {
    private String nomeEntidade;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private long totalRegistros;
    private long registrosUltimas24h;
    private long registrosComNulos;
    private LocalDateTime ultimaExtracao;
    private StatusValidacao status;
    private String erro;
    private List<String> observacoes;
    
    public ResultadoValidacaoEntidade() {
        this.observacoes = new ArrayList<>();
        this.status = StatusValidacao.PENDENTE;
    }
    
    // Getters e Setters
    public String getNomeEntidade() {
        return nomeEntidade;
    }
    
    public void setNomeEntidade(final String nomeEntidade) {
        this.nomeEntidade = nomeEntidade;
    }
    
    public LocalDateTime getDataInicio() {
        return dataInicio;
    }
    
    public void setDataInicio(final LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }
    
    public LocalDateTime getDataFim() {
        return dataFim;
    }
    
    public void setDataFim(final LocalDateTime dataFim) {
        this.dataFim = dataFim;
    }
    
    public long getTotalRegistros() {
        return totalRegistros;
    }
    
    public void setTotalRegistros(final long totalRegistros) {
        this.totalRegistros = totalRegistros;
    }
    
    public long getRegistrosUltimas24h() {
        return registrosUltimas24h;
    }
    
    public void setRegistrosUltimas24h(final long registrosUltimas24h) {
        this.registrosUltimas24h = registrosUltimas24h;
    }
    
    public long getRegistrosComNulos() {
        return registrosComNulos;
    }
    
    public void setRegistrosComNulos(final long registrosComNulos) {
        this.registrosComNulos = registrosComNulos;
    }
    
    public LocalDateTime getUltimaExtracao() {
        return ultimaExtracao;
    }
    
    public void setUltimaExtracao(final LocalDateTime ultimaExtracao) {
        this.ultimaExtracao = ultimaExtracao;
    }
    
    public StatusValidacao getStatus() {
        return status;
    }
    
    public void setStatus(final StatusValidacao status) {
        this.status = status;
    }
    
    public String getErro() {
        return erro;
    }
    
    public void setErro(final String erro) {
        this.erro = erro;
    }
    
    public List<String> getObservacoes() {
        return observacoes;
    }
    
    public void setObservacoes(final List<String> observacoes) {
        this.observacoes = observacoes != null ? observacoes : new ArrayList<>();
    }
    
    public void adicionarObservacao(final String observacao) {
        if (observacao != null && !observacao.trim().isEmpty()) {
            this.observacoes.add(observacao);
        }
    }
    
    /**
     * Verifica se a validação foi bem-sucedida (status OK).
     * 
     * @return true se o status for OK, false caso contrário
     */
    public boolean isValida() {
        return status == StatusValidacao.OK;
    }
    
    /**
     * Verifica se há alertas na validação.
     * 
     * @return true se o status for ALERTA, false caso contrário
     */
    public boolean temAlerta() {
        return status == StatusValidacao.ALERTA;
    }
    
    /**
     * Verifica se houve erro na validação.
     * 
     * @return true se o status for ERRO, false caso contrário
     */
    public boolean temErro() {
        return status == StatusValidacao.ERRO;
    }
    
    /**
     * Retorna um resumo textual do resultado da validação.
     * 
     * @return String com resumo do resultado
     */
    public String getResumo() {
        final StringBuilder resumo = new StringBuilder();
        resumo.append(String.format("Entidade: %s | Status: %s", nomeEntidade, status));
        
        if (totalRegistros > 0) {
            resumo.append(String.format(" | Registros: %d", totalRegistros));
        }
        
        if (registrosUltimas24h > 0) {
            resumo.append(String.format(" | Últimas 24h: %d", registrosUltimas24h));
        }
        
        if (erro != null) {
            resumo.append(String.format(" | Erro: %s", erro));
        }
        
        if (!observacoes.isEmpty()) {
            resumo.append(String.format(" | Observações: %d", observacoes.size()));
        }
        
        return resumo.toString();
    }
    
    @Override
    public String toString() {
        return String.format("ResultadoValidacaoEntidade{entidade='%s', status=%s, registros=%d, erro='%s'}", 
                           nomeEntidade, status, totalRegistros, erro);
    }
}