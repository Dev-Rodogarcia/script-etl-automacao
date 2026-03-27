# Auditoria Técnica ETL (Java 17) — 2026-03-25

## 1. Visão Geral

O repositório está **funcionalmente maduro** em mecanismos de resiliência (retry, circuit breaker, timeout watchdog, page-audit), porém ainda tem pontos com risco real de produção em três frentes: (a) **integridade da extração sob cenários limítrofes de paginação/filtro temporal**, (b) **coerência arquitetural entre camadas**, e (c) **janela de validação API x banco potencialmente inconclusiva em cenários específicos**.

Pontos fortes observados:
- Paginação GraphQL com detecção explícita de cursor repetido, página vazia inconsistente e `hasNextPage=true` sem cursor.
- DataExport com classificação de extração parcial (ex.: lacuna por HTTP 422) em vez de “sucesso silencioso”.
- Persistência com MERGE/UPSERT, `LOCK_TIMEOUT`, batch commit, e métricas de no-op idempotente.
- Orquestração com políticas de falha, timeout por step e execução paralela controlada.

Pontos de atenção sistêmica:
- A camada de integração ainda conhece detalhes de persistência e de logging de auditoria em banco.
- Regras de janela diária usam `LocalDate`/dia fechado em vários fluxos; isso reduz ambiguidade operacional, mas não cobre todos os cenários de atraso de eventos em timezone e late-arrival.
- A validação rápida API x banco é robusta para operação diária, porém aceita estados “OK com diferenças brutas” em cenários específicos e depende da qualidade de `log_extracoes`.

---

## 2. Problemas Críticos (🔴)

### 🔴 C1 — Risco de perda silenciosa por recorte temporal diário fixo (late-arrival fora da janela)
**Evidência técnica**
- DataExport usa janela por dia (`inicioDoDia` / `fimDoDia`) e envia filtro em formato de data (`YYYY-MM-DD - YYYY-MM-DD`), sem granularidade horária no request body.
- GraphQL em partes relevantes também opera por `LocalDate` e intervalos de dia.

**Impacto real**
Se a fonte registrar/retificar dados com atraso fora do dia operacional esperado (ex.: evento de D-1 só fica visível em D+1 depois do fechamento), o item pode não entrar na extração padrão e só aparecer via reconciliação posterior (ou nem isso, se não houver rotina de backfill dedicada para a entidade).

**Onde aparece**
- `DataExportTimeWindowSupport` define janela de início/fim por dia.
- `DataExportRequestBodyFactory` serializa busca por range de datas (sem hora).
- `ClienteApiGraphQL` e suportes por intervalo usam `LocalDate` para filtros por dia.

**Recomendação objetiva**
Implementar estratégia híbrida: janela principal diária + **janela de sobreposição móvel** (ex.: últimas 48–72h) com idempotência forte no banco para absorver atrasos sem duplicar.

### 🔴 C2 — Acoplamento direto Integracao ↔ Persistencia compromete confiabilidade evolutiva
**Evidência técnica**
Serviços de integração (`GraphQLExtractionService` / `DataExportExtractionService`) constroem repositórios concretos e salvam diretamente, sem fronteira de caso de uso/porta para persistência.

**Impacto real**
Mudanças de persistência (schema, estratégia de upsert, multi-DB, transação distribuída) exigem alterações em módulos de integração, elevando risco de regressão justamente no fluxo crítico de ingestão.

**Recomendação objetiva**
Mover coordenação de persistência para camada de aplicação (use cases/orquestrador), expondo na integração apenas gateways de leitura externa + mapeamento. Repositórios entram via portas na aplicação.

---

## 3. Problemas Altos (🟠)

### 🟠 A1 — Validação API x Banco 24h depende de “última janela completa” e pode mascarar drift operacional recente
**Evidência técnica**
- `ValidacaoApiBanco24hUseCase` usa fallback para “última janela COMPLETA” quando não encontra janela ideal do dia.
- A própria implementação documenta que API atual entra como telemetria operacional, não como critério estrito principal.

**Impacto real**
Em incidentes recentes (horas mais próximas), o sistema pode aprovar uma execução baseada em janela anterior completa, reduzindo sensibilidade para detectar atraso recém-introduzido.

**Recomendação objetiva**
Adicionar “modo estrito de frescor” opcional na validação: reprovar se não existir janela completa para a data-alvo e se delta API atual vs log superar threshold.

### 🟠 A2 — Execução condicional de `faturas_graphql` por fase pode gerar cobertura parcial quando comando orquestrador diverge
**Evidência técnica**
No `GraphQLExtractionService`, `faturas_graphql` só executa internamente quando solicitado isoladamente; caso contrário, depende de execução posterior no comando/orquestrador.

**Impacto real**
Se algum fluxo chamar esse serviço diretamente esperando “todas as entidades GraphQL”, pode haver falsa percepção de completude.

**Recomendação objetiva**
Tornar explícito no contrato (nome de método e retorno estruturado) quais entidades foram realmente executadas e quais foram delegadas.

### 🟠 A3 — Estratégia de “continua após erro” em persistência pode consolidar carga parcialmente aplicada
**Evidência técnica**
`AbstractRepository.salvar` pode continuar após falhas individuais dependendo da configuração (`isContinuarAposErro`), com commits em lotes.

**Impacto real**
Em modo não atômico, erros de qualidade de dados podem produzir parcialidade persistente no mesmo ciclo, exigindo reconciliação para fechar lacunas.

**Recomendação objetiva**
Usar modo atômico por entidade crítica (ou por janela) em execução oficial; manter modo tolerante apenas para execução diagnóstica/recovery.

---

## 4. Problemas Médios (🟡)

### 🟡 M1 — Heurística de consistência de paginação GraphQL pode interromper extração em cenários de paginação não uniforme
O `GraphQLPaginator` interrompe quando `hasNextPage=true` e página vem menor que tamanho esperado histórico. Isso é ótimo contra loops/anomalias, mas pode ser agressivo se API legítima variar tamanho de página por filtros internos.

### 🟡 M2 — Alto volume de responsabilidade em services de integração
`GraphQLExtractionService` e `DataExportExtractionService` acumulam orquestração, timeout, seleção de entidade, persistência, log de negócio e política de erro. Isso aumenta custo de manutenção e de testes de regressão.

### 🟡 M3 — Dependência forte de parsing textual em mensagem para parte da validação
A validação rápida extrai métricas também de campos textuais/logados. Quando formato de mensagem muda, risco de ruído/falso negativo aumenta.

---

## 5. Problemas Baixos (🟢)

### 🟢 B1 — Inconsistências de documentação de cabeçalho em arquivos (doc-file antigo)
Há cabeçalhos com caminho/pacote histórico divergente do local atual. Não quebra execução, mas atrapalha auditoria rápida.

### 🟢 B2 — Strings de log com encoding inconsistente
Há logs com caracteres corrompidos em alguns pontos (`⚠️`, `🚨`), sem impacto funcional, mas afeta legibilidade operacional em alguns consoles.

---

## 6. Problemas Específicos de ETL (SEÇÃO OBRIGATÓRIA)

Foco exclusivo: confiabilidade da extração.

1. **Recorte temporal por dia (sem sobreposição móvel nativa)**
   - Risco: perda de registros tardios entre janelas.
   - Ação: sobreposição de 48–72h + deduplicação idempotente por chave natural/versão.

2. **Extração parcial explícita, mas ainda dependente de reação operacional**
   - DataExport marca `LACUNA_PAGINACAO_422` (bom), porém requer monitoramento e reconciliação para fechamento.
   - Ação: automatizar reprocessamento imediato da lacuna no mesmo ciclo quando custo permitir.

3. **Possível parada preventiva em heurística de paginação GraphQL**
   - Segurança alta contra loop, porém pode reduzir completude se a API tiver comportamento legítimo irregular.
   - Ação: adicionar modo “audit-only” para coletar evidência antes de interromper definitivamente.

4. **Cadeia de retries por dia/entidade sem política global de budget de retries do ciclo**
   - Risco: aumento de tempo total, competição por janela operacional e backlog.
   - Ação: impor orçamento global de retry por ciclo e prioridade por entidade crítica.

---

## 7. Sugestões de Arquitetura

1. **Separar “Aquisição” de “Aplicação de estado”**
   - `integracao`: somente chamada API + paginação + parse para DTO de integração.
   - `aplicacao`: regras de janela, idempotência, decisão de retry por entidade.
   - `persistencia`: adaptação final para MERGE/UPSERT.

2. **Ports explícitas para escrita por agregado**
   - Ex.: `SalvarManifestosPort`, `SalvarFretesPort` com contrato de idempotência e resultado detalhado (`inserted/updated/noop/invalid`).

3. **Contrato de completude por entidade**
   - Cada entidade expõe regra declarativa: chave de deduplicação, campo de frescor, tolerância de atraso, política de reconciliação.

4. **Modelo de execução “intent + outcome”**
   - Persistir intenção de extração (janela/entidade/config hash) antes de rodar, e outcome ao final; facilita auditoria e replay seguro.

---

## 8. Sugestões de Organização de Pastas

Estrutura atual por camada funciona, mas para escala de múltiplas versões/entidades ETL, recomendo **híbrido por feature + camada interna**:

```text
src/main/java/br/com/extrator/
  features/
    coletas/
      aplicacao/
      dominio/
      integracao/
      persistencia/
      observabilidade/
    fretes/
    manifestos/
  plataforma/
    bootstrap/
    seguranca/
    suporte/
    observabilidade/
```

Benefícios concretos:
- Reduz mudança transversal em dezenas de pacotes ao evoluir uma única entidade.
- Facilita ownership por time/feature.
- Melhora testabilidade de ponta a ponta por contexto de negócio.

Transição sugerida: começar por uma entidade de alto churn (ex.: `faturas_graphql`) e validar padrão antes de migração ampla.

---

## 9. Riscos Ocultos (IMPORTANTÍSSIMO)

1. **Falso senso de sucesso em ciclos com validação dependente de logs históricos completos**
   - Sem janela completa fresca, fallback pode aprovar estado operacional menos recente.

2. **Mudança externa de comportamento de paginação/filtro da API sem quebra de contrato explícita**
   - O código está preparado para várias inconsistências, mas mudanças “válidas porém diferentes” podem reduzir completude sem erro fatal imediato.

3. **Acoplamento de integração com persistência dificulta rollout de correções emergenciais**
   - Hotfix em regra de banco pode exigir mexer em fluxo de extração e vice-versa.

4. **Execução contínua com retries cumulativos pode pressionar janela operacional**
   - Mesmo com watchdog por ciclo, sem budget de retries por ciclo/entidade o throughput real pode oscilar sob degradação de API.

---

## Conclusão Executiva

O sistema já possui vários mecanismos sólidos de confiabilidade (principalmente contra falhas explícitas), mas os maiores riscos de produção restantes estão em **completude sob atrasos de dados**, **acoplamento arquitetural** e **semântica operacional de validação rápida**. O próximo salto de robustez vem de: (1) janela de sobreposição com idempotência forte, (2) desacoplamento integração↔persistência via portas de aplicação, e (3) validação com critério de frescor estrito opcional.
