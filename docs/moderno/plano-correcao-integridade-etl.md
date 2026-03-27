---
context:
  - ETL
  - Integridade
  - Plano
updated_at: 2026-03-25T00:00:00-03:00
source_of_truth: code
classification: atual
related_files:
  - src/main/java/br/com/extrator/aplicacao/extracao/FluxoCompletoUseCase.java
  - src/main/java/br/com/extrator/plataforma/auditoria/aplicacao/ExecutionWindowPlanner.java
  - src/main/java/br/com/extrator/observabilidade/servicos/IntegridadeEtlValidator.java
  - src/main/java/br/com/extrator/features/usuarios/persistencia/sqlserver/SqlServerUsuariosEstadoRepository.java
---

# Plano de correcao de integridade do ETL

## Problema consolidado

O ETL ja possuia boa resiliencia operacional, mas ainda deixava dois riscos estruturais:

- aprovacao de ciclo baseada em sinais historicos ou textuais demais;
- persistencia de estado que nao representava completamente a verdade operacional de algumas entidades.

Os pontos mais sensiveis eram:

- validacao final dependente de leitura indireta de `log_extracoes`;
- ausencia de uma trilha autorizativa por `execution_uuid`;
- janela operacional sem formalizacao de `watermark` confirmado;
- `usuarios_sistema` tratado como carga sem estado rastreavel.

## Objetivos

1. Aprovar um run apenas quando a execucao atual tiver auditoria estruturada valida.
2. Separar validacao operacional rapida de validacao autorizativa.
3. Formalizar janelas por entidade com `watermark + overlap`.
4. Tratar `usuarios_sistema` como dimensao com estado atual e historico simples.
5. Introduzir a organizacao alvo por `features` e `plataforma` sem big bang.

## Arquitetura alvo

```text
br.com.extrator/
  features/
    coletas/
      aplicacao/
      dominio/
      integracao/graphql/
      persistencia/sqlserver/
    manifestos/
      aplicacao/
      dominio/
      integracao/dataexport/
      persistencia/sqlserver/
    fretes/
      aplicacao/
      dominio/
      integracao/graphql/
      persistencia/sqlserver/
    faturas/
      aplicacao/
      dominio/
      integracao/dataexport/
      persistencia/sqlserver/
    usuarios/
      aplicacao/
      dominio/
      integracao/graphql/
      persistencia/sqlserver/
  plataforma/
    bootstrap/
    pipeline/
    auditoria/
    observabilidade/
    seguranca/
    suporte/
```

## Fases implementadas

### Fase 0. Baseline e protecao contra regressao

- baseline de testes mantido como gate antes da refatoracao;
- `faturas_graphql` permanece fora do escopo de rollout e aceite desta frente.

### Fase 1. Auditoria estruturada por execucao

- criada a porta `ExecutionAuditPort`;
- criada a persistencia em `sys_execution_audit`;
- criado `ExecutionAuditRecorder` para gravar a trilha estruturada por entidade;
- `log_extracoes` continua como telemetria humana, nao como contrato autorizativo.

### Fase 2. Separacao de validadores

- o `FluxoCompletoUseCase` deixou de autorizar run por validacao rapida;
- a aprovacao final passou a depender de `IntegridadeEtlValidator` com `execution_uuid`;
- se `sys_execution_audit` ou `execution_uuid` nao estiverem disponiveis, a validacao reprova em vez de cair para fallback legado.

### Fase 3. Janela com watermark e overlap

- criado `ExecutionWindowPlanner`;
- criadas estrategias por feature para `coletas`, `manifestos`, `fretes` e `usuarios_sistema`;
- o ciclo passa a planejar `janela de consulta` e `janela de confirmacao`;
- `watermark` confirmado fica em `sys_execution_watermark`.

### Fase 4. Usuarios com estado rastreavel

- `usuarios_sistema` passou a usar snapshot semantico;
- `dim_usuarios` ganhou colunas de estado;
- `dim_usuarios_historico` registra mudancas relevantes por execucao;
- usuario desabilitado nao some silenciosamente do destino.

### Fase 5. Organizacao por feature

- foram introduzidos os pacotes `features/*` e `plataforma/*` para a nova trilha;
- a migracao continua incremental: componentes novos entram no destino final, enquanto o legado segue drenado sem big bang.

## Criterios de aceite

- existe `execution_uuid` correlacionando a execucao autorizada;
- a validacao final nao usa regex em `mensagem`;
- `sys_execution_audit` e `sys_execution_watermark` estao ativos;
- `usuarios_sistema` registra estado atual e historico;
- o daemon continua operando com `--sem-faturas-graphql`.

## Riscos residuais

- entidades ainda nao migradas por feature continuam vivendo em pacotes tecnicos legados;
- `faturas_graphql` continua fora desta frente para nao alongar homologacao;
- a qualidade da auditoria estruturada depende da aplicacao dos scripts `014` a `017` no banco.

## Ordem de implantacao

1. aplicar scripts SQL `014` a `017`;
2. validar compilacao e testes em janela com daemon parado;
3. executar extracao controlada sem `faturas_graphql`;
4. rodar validacao autorizativa do run;
5. religar o daemon com o mesmo escopo operacional.
