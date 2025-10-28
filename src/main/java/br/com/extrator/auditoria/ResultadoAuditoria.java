package br.com.extrator.auditoria;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ResultadoAuditoria {

    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private List<String> resultadosValidacao;
    private StatusAuditoria statusGeral;
    private String erro;

    public ResultadoAuditoria() {
        this.dataInicio = LocalDateTime.now();
        this.resultadosValidacao = new ArrayList<>();
    }

    public ResultadoAuditoria(LocalDateTime dataInicio, LocalDateTime dataFim, List<String> resultadosValidacao) {
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.resultadosValidacao = resultadosValidacao != null ? resultadosValidacao : new ArrayList<>();
    }

    // Getters e Setters
    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDateTime getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDateTime dataFim) {
        this.dataFim = dataFim;
    }

    public List<String> getResultadosValidacao() {
        return resultadosValidacao;
    }

    public void setResultadosValidacao(List<String> resultadosValidacao) {
        this.resultadosValidacao = resultadosValidacao;
    }

    public StatusAuditoria getStatusGeral() {
        return statusGeral;
    }

    public void setStatusGeral(StatusAuditoria statusGeral) {
        this.statusGeral = statusGeral;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }

    public void setDataExecucao(LocalDateTime dataExecucao) {
        this.dataInicio = dataExecucao;
    }

    public void adicionarValidacao(String entidade, ResultadoValidacaoEntidade resultado) {
        if (this.resultadosValidacao == null) {
            this.resultadosValidacao = new ArrayList<>();
        }
        this.resultadosValidacao.add(entidade + ": " + resultado.toString());
    }

    public void determinarStatusGeral() {
        if (erro != null && !erro.isEmpty()) {
            this.statusGeral = StatusAuditoria.ERRO;
        } else if (resultadosValidacao.stream().anyMatch(r -> r.toLowerCase().contains("erro"))) {
            this.statusGeral = StatusAuditoria.CONCLUIDA_COM_ALERTAS;
        } else {
            this.statusGeral = StatusAuditoria.CONCLUIDA;
        }
    }

    // Método utilitário opcional (pode estar sendo chamado em AuditoriaRelatorio)
    public void adicionarResultado(String resultado) {
        if (this.resultadosValidacao == null) {
            this.resultadosValidacao = new ArrayList<>();
        }
        this.resultadosValidacao.add(resultado);
    }

    public boolean isSucesso() {
        // Exemplo simples de lógica
        return resultadosValidacao.stream().noneMatch(r -> r.toLowerCase().contains("erro"));
    }

    @Override
    public String toString() {
        return "ResultadoAuditoria{" +
                "dataInicio=" + dataInicio +
                ", dataFim=" + dataFim +
                ", resultadosValidacao=" + resultadosValidacao +
                ", statusGeral=" + statusGeral +
                ", erro='" + erro + '\'' +
                '}';
    }
}