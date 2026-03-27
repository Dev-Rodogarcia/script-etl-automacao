/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/aplicacao/pipeline/runtime/StepExecutionResult.java
Classe  : StepExecutionResult (class)
Pacote  : br.com.extrator.aplicacao.pipeline.runtime
Modulo  : Pipeline - Runtime

Papel   : Value object imutavel que encapsula resultado de execucao de um step (status, timing, erro, metadata).

Conecta com:
- ErrorTaxonomy (classifica erro)
- StepStatus (enum de estado)

Fluxo geral:
1) Step executa e constroi resultado via builder pattern.
2) Resultado inclui: nome, entidade, status, timestamps, tentativa, mensagem, taxonomy, metadata.
3) PipelineOrchestrator consulta resultado para decidir próximas acoes.

Estrutura interna:
Builder (fluent API):
- stepName, entityName, status, startedAt, finishedAt, attempt, message, errorTaxonomy, metadata.
Metodos principais:
- builder(stepName, entityName): factory para construtor fluent.
- getStatus(), getMessage(), getMetadata(): accessores.
- durationMillis(): calcula duracao entre start e finish.
- isSuccess(), isFailed(): predicados de estado.
Atributos-chave:
- status: StepStatus (SUCCESS, FAILED, SKIPPED, DEGRADED).
- errorTaxonomy: ErrorTaxonomy (classifica tipo de erro).
- metadata: Map imutavel com dados adicionais (ex: check_failed).
[DOC-FILE-END]============================================================== */
package br.com.extrator.aplicacao.pipeline.runtime;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import br.com.extrator.aplicacao.politicas.ErrorTaxonomy;

public final class StepExecutionResult {
    private final String stepName;
    private final String entityName;
    private final StepStatus status;
    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;
    private final int attempt;
    private final String message;
    private final ErrorTaxonomy errorTaxonomy;
    private final Map<String, Object> metadata;

    private StepExecutionResult(final Builder builder) {
        this.stepName = builder.stepName;
        this.entityName = builder.entityName;
        this.status = builder.status;
        this.startedAt = builder.startedAt;
        this.finishedAt = builder.finishedAt;
        this.attempt = builder.attempt;
        this.message = builder.message;
        this.errorTaxonomy = builder.errorTaxonomy;
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(builder.metadata));
    }

    public static Builder builder(final String stepName, final String entityName) {
        return new Builder(stepName, entityName);
    }

    public String obterNomeEtapa() {
        return stepName;
    }

    public String obterNomeEntidade() {
        return entityName;
    }

    public StepStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public int getAttempt() {
        return attempt;
    }

    public String getMessage() {
        return message;
    }

    public ErrorTaxonomy getErrorTaxonomy() {
        return errorTaxonomy;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public long durationMillis() {
        return Duration.between(startedAt, finishedAt).toMillis();
    }

    public boolean isSuccess() {
        return status == StepStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == StepStatus.FAILED;
    }

    public static final class Builder {
        private final String stepName;
        private final String entityName;
        private StepStatus status = StepStatus.SUCCESS;
        private LocalDateTime startedAt = LocalDateTime.now();
        private LocalDateTime finishedAt = LocalDateTime.now();
        private int attempt = 1;
        private String message = "";
        private ErrorTaxonomy errorTaxonomy = null;
        private final Map<String, Object> metadata = new LinkedHashMap<>();

        private Builder(final String stepName, final String entityName) {
            this.stepName = stepName;
            this.entityName = entityName;
        }

        public Builder status(final StepStatus status) {
            this.status = status;
            return this;
        }

        public Builder startedAt(final LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder finishedAt(final LocalDateTime finishedAt) {
            this.finishedAt = finishedAt;
            return this;
        }

        public Builder attempt(final int attempt) {
            this.attempt = attempt;
            return this;
        }

        public Builder message(final String message) {
            this.message = message;
            return this;
        }

        public Builder errorTaxonomy(final ErrorTaxonomy errorTaxonomy) {
            this.errorTaxonomy = errorTaxonomy;
            return this;
        }

        public Builder metadata(final String key, final Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public StepExecutionResult build() {
            return new StepExecutionResult(this);
        }
    }
}


