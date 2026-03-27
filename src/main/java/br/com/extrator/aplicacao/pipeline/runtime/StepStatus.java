/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/aplicacao/pipeline/runtime/StepStatus.java
Classe  : StepStatus (enum)
Pacote  : br.com.extrator.aplicacao.pipeline.runtime
Modulo  : Pipeline - Runtime

Papel   : Enum que representa estado de execucao de um step no pipeline.

Conecta com:
- StepExecutionResult (usa status)

Fluxo geral:
1) Step termina com um dos estados: SUCCESS, FAILED, SKIPPED, DEGRADED.
2) PipelineOrchestrator usa status para decidir continuacao.
3) SUCCESS/DEGRADED = pode continuar; FAILED = aborta (conforme politica).

Estrutura interna:
Valores:
- SUCCESS: step concluiu normalmente.
- FAILED: step falhou (erro critico).
- SKIPPED: step nao foi executado (condicao nao atendida).
- DEGRADED: step concluiu mas com problemas (resultado parcial, avisos).
[DOC-FILE-END]============================================================== */
package br.com.extrator.aplicacao.pipeline.runtime;

public enum StepStatus {
    SUCCESS,
    FAILED,
    SKIPPED,
    DEGRADED
}
