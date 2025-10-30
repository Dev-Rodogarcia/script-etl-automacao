package br.com.extrator.auditoria;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe que representa o resultado da validação de uma entidade específica.
 * Contém informações sobre a completude dos dados, estatísticas e observações.
 */
public class ResultadoValidacaoEntidade {
    private String nomeEntidade;
    private Instant dataInicio;
    private Instant dataFim;
    private long totalRegistros;
    private long registrosUltimas24h;
    private long registrosComNulos;
    private Instant ultimaExtracao;
    private StatusValidacao status;
    private String erro;
    private List<String> observacoes;
    
    // Novos campos para debug e causa raiz
    private String colunaUtilizada;
    private String queryExecutada;
    
    public ResultadoValidacaoEntidade() {
        this.observacoes = new ArrayList<>();
        this.status = StatusValidacao.PENDENTE;
    }
    
    // ✅ MÉTODOS ESTÁTICOS PARA CRIAÇÃO PADRONIZADA
    
    /**
     * Cria um resultado de validação com status de ERRO.
     * 
     * @param entidade Nome da entidade
     * @param registros Número de registros encontrados
     * @param mensagem Mensagem de erro
     * @return ResultadoValidacaoEntidade com status ERRO
     */
    public static ResultadoValidacaoEntidade erro(String entidade, long registros, String mensagem) {
        ResultadoValidacaoEntidade resultado = new ResultadoValidacaoEntidade();
        resultado.setNomeEntidade(entidade);
        resultado.setTotalRegistros(registros);
        resultado.setStatus(StatusValidacao.ERRO);
        resultado.setErro(mensagem);
        return resultado;
    }
    
    /**
     * Cria um resultado de validação com status de ALERTA.
     * 
     * @param entidade Nome da entidade
     * @param registros Número de registros encontrados
     * @param mensagem Mensagem de alerta
     * @return ResultadoValidacaoEntidade com status ALERTA
     */
    public static ResultadoValidacaoEntidade alerta(String entidade, long registros, String mensagem) {
        ResultadoValidacaoEntidade resultado = new ResultadoValidacaoEntidade();
        resultado.setNomeEntidade(entidade);
        resultado.setTotalRegistros(registros);
        resultado.setStatus(StatusValidacao.ALERTA);
        resultado.adicionarObservacao(mensagem);
        return resultado;
    }
    
    /**
     * Cria um resultado de validação com status OK.
     * 
     * @param entidade Nome da entidade
     * @param registros Número de registros encontrados
     * @return ResultadoValidacaoEntidade com status OK
     */
    public static ResultadoValidacaoEntidade ok(String entidade, long registros) {
        ResultadoValidacaoEntidade resultado = new ResultadoValidacaoEntidade();
        resultado.setNomeEntidade(entidade);
        resultado.setTotalRegistros(registros);
        resultado.setStatus(StatusValidacao.OK);
        return resultado;
    }
    
    // Getters e Setters
    public String getNomeEntidade() {
        return nomeEntidade;
    }
    
    public void setNomeEntidade(final String nomeEntidade) {
        this.nomeEntidade = nomeEntidade;
    }
    
    public Instant getDataInicio() {
        return dataInicio;
    }
    
    public void setDataInicio(final Instant dataInicio) {
        this.dataInicio = dataInicio;
    }
    
    public Instant getDataFim() {
        return dataFim;
    }
    
    public void setDataFim(final Instant dataFim) {
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
    
    public Instant getUltimaExtracao() {
        return ultimaExtracao;
    }
    
    public void setUltimaExtracao(final Instant ultimaExtracao) {
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
    
    public String getColunaUtilizada() {
        return colunaUtilizada;
    }
    
    public void setColunaUtilizada(final String colunaUtilizada) {
        this.colunaUtilizada = colunaUtilizada;
    }
    
    public String getQueryExecutada() {
        return queryExecutada;
    }
    
    public void setQueryExecutada(final String queryExecutada) {
        this.queryExecutada = queryExecutada;
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