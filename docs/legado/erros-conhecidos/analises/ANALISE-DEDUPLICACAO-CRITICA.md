---
context:
  - Documentacao
  - Legado
  - ArquivoHistorico
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: docs
classification: parcial
related_files:
  - docs/index.md
  - docs/legado/index.md
  - docs/legado/classificacao.md
---
# 🔍 Análise Crítica da Deduplicação - Problemas Identificados

**Data da Análise:** 26/01/2026  
**Data da Correção:** 03/02/2026  
**Status:** ✅ **CORREÇÕES APLICADAS**  
**Severidade:** ✅ **RESOLVIDO**

---

## 📋 RESUMO EXECUTIVO

**ATUALIZAÇÃO (03/02/2026):** Todas as correções recomendadas nesta análise foram implementadas no código. Este documento permanece como histórico da análise e das melhorias aplicadas.

### Correções Implementadas:

✅ **Estratégia "Keep Last"**: A deduplicação agora mantém o registro mais recente baseado em timestamps (`finishedAt`, `requestedAt`, `serviceAt`) ou o último processado quando não há timestamps.

✅ **Chave de Manifestos Alinhada**: A chave de deduplicação de Manifestos agora usa `(sequence_code, pick_sequence_code, mdfe_number)`, alinhada com a chave do MERGE SQL.

✅ **Comparação de Dados**: O sistema compara timestamps antes de decidir qual registro manter, preservando dados atualizados.

---

## 🚨 PROBLEMAS IDENTIFICADOS (HISTÓRICO - JÁ CORRIGIDOS)

### 1. **Deduplicação Mantinha SEMPRE o Primeiro Registro** ✅ CORRIGIDO

---

## 🚨 PROBLEMAS CRÍTICOS IDENTIFICADOS

### 1. **Deduplicação Mantém SEMPRE o Primeiro Registro**

**Problema:**
```java
(primeiro, segundo) -> {
    logger.warn("⚠️ Duplicado detectado na resposta da API: sequence_code={}", 
        segundo.getSequenceCode());
    return primeiro; // ⚠️ SEMPRE mantém o primeiro, descarta o segundo
}
```

**Risco:**
- Se a API retornar o mesmo registro múltiplas vezes com **dados atualizados**, perdemos os dados mais recentes
- Exemplo: Manifesto `48831` aparece 3 vezes:
  1. Primeira vez: `status = "pending"`, `finished_at = NULL`
  2. Segunda vez: `status = "in_transit"`, `finished_at = "2026-01-26 10:00"`
  3. Terceira vez: `status = "finished"`, `finished_at = "2026-01-26 15:00"`
  
  **Resultado atual:** Mantém apenas a primeira (status pending) ❌
  **Resultado esperado:** Deveria manter a última (status finished) ✅

**Impacto:** 🔴 **ALTO** - Dados desatualizados no banco

---

### 2. **Inconsistência entre Deduplicação e MERGE (Manifestos)**

**Problema:**
- **Deduplicação usa:** `sequence_code + "_" + identificador_unico`
- **MERGE usa:** `(sequence_code, pick_sequence_code, mdfe_number)`

**Exemplo:**
```
Manifesto 1: sequence_code=48831, pick_sequence_code=71920, mdfe_number=NULL
Manifesto 2: sequence_code=48831, pick_sequence_code=71920, mdfe_number=1503
```

- **Deduplicação:** Considera diferentes (identificador_unico diferente) ✅
- **MERGE:** Considera diferentes (mdfe_number diferente) ✅
- **MAS:** Se `identificador_unico` for calculado incorretamente, pode haver inconsistência

**Impacto:** 🟡 **MÉDIO** - Pode causar duplicados no banco ou perda de registros

---

### 3. **Falta de Validação de Dados Antes de Descartar**

**Problema:**
- A deduplicação não compara se os registros são **realmente idênticos**
- Pode descartar registros com dados diferentes mas mesma chave

**Exemplo:**
```
Cotação 1: sequence_code=12345, total_value=1000.00, status="pending"
Cotação 2: sequence_code=12345, total_value=1200.00, status="approved"
```

- **Deduplicação atual:** Descarta a segunda (mesmo sequence_code)
- **Problema:** São registros diferentes! A segunda tem dados atualizados

**Impacto:** 🔴 **ALTO** - Perda de dados atualizados

---

### 4. **GraphQL Não Tem Deduplicação (Mas Pode Precisar)**

**Situação Atual:**
- GraphQL não aplica deduplicação antes de salvar
- Confia apenas no MERGE usando `id` como chave

**Risco:**
- Se a API GraphQL começar a retornar duplicados (como DataExport), não teremos proteção
- O MERGE atualiza registros existentes, mas se houver duplicados na mesma resposta, pode causar:
  - Múltiplas execuções de MERGE desnecessárias
  - Performance degradada

**Impacto:** 🟡 **MÉDIO** - Performance e possível inconsistência futura

---

## ✅ SOLUÇÕES PROPOSTAS

### Solução 1: Deduplicação Inteligente (Recomendada)

**Estratégia:** Comparar dados antes de descartar

```java
public static List<ManifestoEntity> deduplicarManifestos(final List<ManifestoEntity> manifestos) {
    return manifestos.stream()
        .collect(Collectors.toMap(
            m -> m.getSequenceCode() + "_" + m.getIdentificadorUnico(),
            m -> m,
            (primeiro, segundo) -> {
                // ✅ COMPARAR DADOS antes de decidir qual manter
                if (saoIdenticos(primeiro, segundo)) {
                    // São idênticos, manter o primeiro
                    logger.debug("Registros idênticos detectados: sequence_code={}", 
                        primeiro.getSequenceCode());
                    return primeiro;
                } else {
                    // São diferentes, manter o mais recente (baseado em data_extracao ou finished_at)
                    final ManifestoEntity maisRecente = obterMaisRecente(primeiro, segundo);
                    logger.warn("⚠️ Duplicado com dados diferentes: sequence_code={}, mantendo o mais recente", 
                        primeiro.getSequenceCode());
                    return maisRecente;
                }
            }
        ))
        .values()
        .stream()
        .collect(Collectors.toList());
}
```

**Benefícios:**
- ✅ Preserva dados atualizados
- ✅ Evita perda de informação
- ✅ Mais seguro

---

### Solução 2: Alinhar Deduplicação com MERGE

**Estratégia:** Usar a mesma chave na deduplicação e no MERGE

```java
// Para Manifestos: usar (sequence_code, pick_sequence_code, mdfe_number)
public static List<ManifestoEntity> deduplicarManifestos(final List<ManifestoEntity> manifestos) {
    return manifestos.stream()
        .collect(Collectors.toMap(
            m -> {
                // ✅ Usar a mesma chave do MERGE
                return String.format("%d_%d_%d", 
                    m.getSequenceCode(),
                    m.getPickSequenceCode() != null ? m.getPickSequenceCode() : -1,
                    m.getMdfeNumber() != null ? m.getMdfeNumber() : -1);
            },
            m -> m,
            (primeiro, segundo) -> obterMaisRecente(primeiro, segundo)
        ))
        .values()
        .stream()
        .collect(Collectors.toList());
}
```

**Benefícios:**
- ✅ Consistência entre deduplicação e MERGE
- ✅ Evita duplicados no banco
- ✅ Mais previsível

---

### Solução 3: Adicionar Deduplicação Preventiva para GraphQL

**Estratégia:** Adicionar deduplicação opcional para GraphQL

```java
// Em ColetaExtractor, FretExtractor, etc.
@Override
public int save(final List<ColetaNodeDTO> dtos) throws SQLException {
    if (dtos == null || dtos.isEmpty()) {
        return 0;
    }
    
    final List<ColetaEntity> entities = dtos.stream()
        .map(mapper::toEntity)
        .collect(Collectors.toList());
    
    // ✅ Deduplicação preventiva (opcional, mas recomendada)
    final List<ColetaEntity> entitiesUnicos = deduplicarPorId(entities);
    
    if (entities.size() != entitiesUnicos.size()) {
        log.warn("⚠️ Duplicados detectados na resposta GraphQL: {} removidos", 
            entities.size() - entitiesUnicos.size());
    }
    
    return repository.salvar(entitiesUnicos);
}

private List<ColetaEntity> deduplicarPorId(final List<ColetaEntity> entities) {
    return entities.stream()
        .collect(Collectors.toMap(
            ColetaEntity::getId,
            e -> e,
            (primeiro, segundo) -> {
                // Se IDs são iguais, comparar dados
                if (saoIdenticos(primeiro, segundo)) {
                    return primeiro;
                } else {
                    log.warn("⚠️ Duplicado com dados diferentes: id={}", primeiro.getId());
                    return obterMaisRecente(primeiro, segundo);
                }
            }
        ))
        .values()
        .stream()
        .collect(Collectors.toList());
}
```

**Benefícios:**
- ✅ Proteção preventiva contra duplicados
- ✅ Melhor performance (menos MERGEs)
- ✅ Consistência com DataExport

---

## 📊 PRIORIZAÇÃO DE CORREÇÕES

| Prioridade | Problema | Impacto | Esforço | Recomendação |
|------------|----------|---------|---------|--------------|
| 🔴 **P0** | Deduplicação mantém sempre o primeiro | ALTO | MÉDIO | **CORRIGIR IMEDIATAMENTE** |
| 🟡 **P1** | Inconsistência Manifestos (dedup vs MERGE) | MÉDIO | BAIXO | Corrigir alinhamento |
| 🟡 **P2** | Falta validação antes de descartar | ALTO | MÉDIO | Implementar comparação |
| 🟢 **P3** | GraphQL sem deduplicação | BAIXO | BAIXO | Adicionar preventivamente |

---

## 🎯 RECOMENDAÇÃO FINAL

**Implementar Solução 1 + Solução 2** para todas as entidades DataExport:
1. ✅ Comparar dados antes de descartar
2. ✅ Manter o registro mais recente quando diferentes
3. ✅ Alinhar chave de deduplicação com chave do MERGE
4. ✅ Adicionar logs detalhados para auditoria

**Para GraphQL:**
- Adicionar deduplicação preventiva (Solução 3)
- Usar `id` como chave (já é único)

---

## 📝 PRÓXIMOS PASSOS (ATUALIZAÇÃO 03/02/2026)

1. [x] ✅ Implementar `obterMaisRecente()` para cada entidade - **CONCLUÍDO**
2. [x] ✅ Atualizar `Deduplicator.java` com lógica inteligente - **CONCLUÍDO**
3. [x] ✅ Alinhar chaves de deduplicação com MERGE - **CONCLUÍDO**
4. [x] ✅ Testar com dados reais - **CONCLUÍDO**
5. [x] ✅ Documentar mudanças - **CONCLUÍDO**
6. [ ] Adicionar deduplicação preventiva para GraphQL - **OPCIONAL (P3 - Baixa prioridade)**

---

**✅ CORREÇÕES APLICADAS:** Todos os problemas críticos identificados nesta análise foram corrigidos. O sistema agora usa estratégia "Keep Last" e chaves alinhadas ao MERGE SQL. Este documento permanece como histórico e referência das melhorias implementadas.
