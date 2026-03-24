---
context:
  - ETL
  - Watchdog
  - Concurrency
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/suporte/concorrencia/OperationTimeoutGuard.java
  - src/main/java/br/com/extrator/suporte/concorrencia/ThreadLeakDetector.java
  - src/main/java/br/com/extrator/comandos/cli/extracao/daemon/LoopDaemonRunHandler.java
---

# Watchdog

## O que e

No contexto deste projeto, watchdog e o conjunto de protecoes que impede uma operacao de ficar presa indefinidamente.

## Componente principal

`OperationTimeoutGuard`:

- executa a tarefa em executor dedicado;
- aguarda com timeout;
- cancela a future;
- tenta encerrar o executor;
- inspeciona se sobrou thread residual;
- converte o problema em `ExecutionTimeoutException` ou `ThreadLeakDetectedException`.

## Por que isso existe

Apenas chamar `interrupt()` nao basta quando:

- codigo externo ignora interrupcao;
- future fica preso;
- thread auxiliar continua viva;
- um processo interno trava silenciosamente.

## Onde o watchdog age

### No pipeline

- cada step passa pelo `OperationTimeoutGuard`.

### No loop daemon

- o ciclo inteiro tambem tem timeout global.

### Em validacoes de resiliencia

- a bateria `--validar-etl-resiliencia` tenta reproduzir travas e vazamentos de thread de forma controlada.

## Sequencia simplificada

```text
iniciar executor dedicado
  -> rodar tarefa
  -> esperar ate timeout
  -> se estourar:
       cancelar future
       desligar executor
       verificar thread residual
       falhar com excecao de timeout/watchdog
```

## Sinal operacional

Se o daemon falha por watchdog, isso nao e "lentidao normal". Significa que a protecao temporal do ciclo considerou que a operacao saiu da faixa segura.
