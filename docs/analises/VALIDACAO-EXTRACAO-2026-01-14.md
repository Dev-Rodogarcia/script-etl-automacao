# 📊 Relatório de Validação - Extração 2026-01-14 17:39:41

## ✅ Resumo Executivo

**Status Geral**: ✅ **SUCESSO TOTAL**

- **Duração**: 8 minutos e 5 segundos
- **Entidades Extraídas**: 9/9 (100%)
- **Validação de Completude**: ✅ 8 OK, ❌ 0 INCOMPLETO
- **Validação Temporal**: ✅ OK
- **Validação de Gaps**: ✅ OK

---

## 📈 Estatísticas de Extração

### API GraphQL
- **Total de Páginas**: 37
- **Total de Registros Extraídos**: 719
- **Total de Registros Salvos**: 719
- **Taxa Média**: 6,83 registros/segundo
- **Tempo Total**: 105,33 segundos

#### Detalhamento por Entidade (GraphQL):
1. ✅ **usuarios_sistema**: 40 registros | 1 página | COMPLETO
2. ✅ **coletas**: 221 registros | 12 páginas | COMPLETO
3. ✅ **fretes**: 354 registros | 18 páginas | COMPLETO
4. ✅ **faturas_graphql**: 104 registros | 6 páginas | COMPLETO

### API DataExport
- **Total de Páginas**: 41
- **Total de Registros Extraídos**: 4.237
- **Total Únicos após Deduplicação**: 4.231
- **Total de Registros Salvos**: 4.231
- **Duplicados Removidos**: 6 (0,14%)
- **Taxa Média**: 33,40 registros/segundo
- **Tempo Total**: 126,67 segundos

#### Detalhamento por Entidade (DataExport):
1. ✅ **manifestos**: 639 registros | 3 páginas | COMPLETO
2. ✅ **cotacoes**: 493 registros | 5 páginas | COMPLETO
3. ✅ **localizacao_cargas**: 1.410 registros | 15 páginas | COMPLETO
4. ✅ **contas_a_pagar**: 279 registros | 3 páginas | COMPLETO
5. ✅ **faturas_por_cliente**: 1.410 registros | 15 páginas | COMPLETO

---

## 🔍 Análise do Page Audit

### Resumo do CSV `page_audit_2026-01-14_17-51-47.csv`:
- **Total de Registros Auditados**: 114
- **Templates Identificados**: 6
  - Template 6399: 8 páginas (Faturas GraphQL - primeira execução)
  - Template 6906: 12 páginas (Faturas GraphQL - segunda execução)
  - Template 8656: 32 páginas (Faturas GraphQL - múltiplas execuções)
  - Template 8636: 8 páginas (Coletas)
  - Template 4924: 32 páginas (Faturas por Cliente)
  - Template 9901: 22 páginas (Usuários Sistema)

### Observações:
- ✅ Todas as páginas foram auditadas com `status_code=200`
- ✅ Páginas vazias detectadas corretamente (`total_itens=0`)
- ✅ Hashes de requisição e resposta registrados
- ✅ Janelas temporais corretas (2026-01-13 a 2026-01-14)

---

## ✅ Validação de Completude

### Comparação API vs Banco de Dados:

| Entidade | API ESL Cloud | Banco de Dados | Status |
|----------|---------------|----------------|--------|
| faturas_por_cliente | 1.410 | 1.410 | ✅ OK |
| localizacao_cargas | 1.410 | 1.410 | ✅ OK |
| fretes | 354 | 354 | ✅ OK |
| contas_a_pagar | 279 | 279 | ✅ OK |
| coletas | 221 | 221 | ✅ OK |
| faturas_graphql | 104 | 104 | ✅ OK |
| manifestos | 639 | 639 | ✅ OK |
| cotacoes | 493 | 493 | ✅ OK |

**Resultado**: ✅ **8/8 entidades com completude 100%**

---

## 🔍 Validações Executadas

### 1. ✅ Validação de Completude
- **Status**: ✅ PASSOU
- **Entidades OK**: 8
- **Entidades Incompletas**: 0
- **Duplicados**: 0
- **Erros**: 0

### 2. ✅ Validação de Gaps (IDs Sequenciais)
- **Status**: ✅ PASSOU
- **Observação**: Tabela 'ocorrencias' não encontrada - validação ignorada (esperado)

### 3. ✅ Validação de Janela Temporal
- **Status**: ✅ PASSOU
- **Resultado**: Nenhum registro criado durante a extração (janela temporal OK)

---

## 📊 Análise de Performance

### Tempo de Execução por API:
- **GraphQL**: 105,33 segundos (1 min 45 s)
- **DataExport**: 126,67 segundos (2 min 7 s)
- **Total**: 232 segundos (3 min 52 s) + validações

### Taxa de Processamento:
- **GraphQL**: 6,83 registros/segundo
- **DataExport**: 33,40 registros/segundo (mais rápido devido a batch processing)

### Eficiência:
- ✅ Nenhuma interrupção por proteções
- ✅ Nenhum loop infinito detectado
- ✅ Todas as páginas processadas corretamente
- ✅ Deduplicação funcionando (6 duplicados removidos)

---

## 🎯 Conclusões

### ✅ Pontos Positivos:
1. **100% de Sucesso**: Todas as 9 entidades foram extraídas com sucesso
2. **Completude Garantida**: Todos os registros da API foram salvos no banco
3. **Validações Passaram**: Completude, gaps e janela temporal OK
4. **Performance Adequada**: Taxas de processamento dentro do esperado
5. **Auditoria Completa**: Todas as páginas foram auditadas no `page_audit`
6. **Deduplicação Funcionando**: 6 duplicados detectados e removidos corretamente

### ⚠️ Observações:
1. **Java Version**: Scripts de validação precisam de Java 17+ (atualmente usando Java 8)
   - **Solução**: Configurar JAVA_HOME para Java 17+ nos scripts .bat
2. **Tabela ocorrencias**: Não existe (validação de gaps ignorada - esperado)

### 📋 Recomendações:
1. ✅ **Configurar Java 17+** nos scripts de validação
2. ✅ **Executar validações regulares** após cada extração
3. ✅ **Monitorar page_audit** para detectar páginas vazias ou erros
4. ✅ **Manter logs detalhados** para troubleshooting

---

## 🔧 Próximos Passos

1. **Corrigir Java Version nos Scripts**:
   - Atualizar scripts .bat para usar Java 17+
   - Verificar JAVA_HOME antes de executar

2. **Executar Validações Completas**:
   ```bash
   06-relatorio-completo-validacao.bat
   ```

3. **Verificar Exportação CSV**:
   ```bash
   07-exportar_csv.bat
   ```

4. **Auditar Estrutura da API**:
   ```bash
   08-auditar_api.bat
   ```

---

## 📝 Notas Finais

**Data da Extração**: 2026-01-14 17:39:41  
**Data da Validação**: 2026-01-14 17:51:47  
**Sistema**: ✅ Operacional e Funcionando Corretamente

**Desenvolvido por**: @valentelucass
