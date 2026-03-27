---
context:
  - ETL
  - Daemon
  - Operacao
updated_at: 2026-03-25T00:00:00-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/comandos/cli/extracao/daemon/DaemonLifecycleService.java
  - src/main/java/br/com/extrator/comandos/cli/extracao/daemon/DaemonStateStore.java
  - scripts/windows/05-loop_extracao_30min.bat
---

# Runbook do daemon

## Identificacao do daemon atual

- runtime do daemon: `logs/daemon/runtime/*.jar`
- pid file: `logs/daemon/loop_daemon.pid`
- state file: `logs/daemon/loop_daemon.state`
- historico por ciclo: `logs/daemon/ciclos/*`

O escopo operacional esperado desta frente e:

```text
--loop-daemon-run --sem-faturas-graphql
```

## Procedimento oficial de parada

### 1. Consultar status

```bat
java -jar target/extrator.jar --loop-daemon-status
```

Confirmar:

- estado atual;
- PID informado;
- jar em uso dentro de `logs/daemon/runtime/`.

### 2. Solicitar parada graciosa

```bat
java -jar target/extrator.jar --loop-daemon-stop
```

### 3. Confirmar que parou

Verificar novamente:

```bat
java -jar target/extrator.jar --loop-daemon-status
```

Checklist:

- `status=STOPPED` no `loop_daemon.state`;
- processo Java do PID anterior nao esta mais vivo;
- nenhum ciclo novo esta sendo escrito em `logs/daemon/ciclos/`.

## Fallback operacional

Usar apenas se a parada graciosa falhar:

1. ler PID de `logs/daemon/loop_daemon.pid`;
2. encerrar o processo Java correspondente;
3. registrar incidente operacional;
4. repetir `--loop-daemon-status` ate `STOPPED`.

## Janela de manutencao

Com o daemon parado:

1. aplicar scripts SQL;
2. compilar;
3. rodar testes em escopo;
4. executar validacao offline;
5. somente depois religar.

## Religamento

```bat
java -jar target/extrator.jar --loop-daemon-start --sem-faturas-graphql
```

## Padrao de Java

- o runtime padrao do projeto e Java 17;
- scripts Windows devem priorizar `JAVA_HOME` apontando para um JDK 17;
- a aplicacao nao depende de `--enable-native-access`.

## Checklist pos-subida

- `--loop-daemon-status` responde `RUNNING`;
- PID novo foi criado;
- jar novo existe em `logs/daemon/runtime/`;
- primeiro ciclo escreve log proprio;
- `loop_daemon.state` foi atualizado;
- o escopo continua sem `faturas_graphql`.
