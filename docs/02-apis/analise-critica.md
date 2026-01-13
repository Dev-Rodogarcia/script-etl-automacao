# 📊 Análise Crítica: Descoberta de Endpoints - Projeto de Automação

**Data:** 06 de Novembro de 2025  
**Analista:** Revisão Técnica Completa  
**Escopo:** 8 entidades mapeadas (Fretes, Coletas, Cotações, Manifestos, Localizador de Cargas, Faturas a Pagar, Faturas a Receber, Ocorrências)

---

## 🎯 **RESUMO EXECUTIVO**

### ✅ **Sucessos Alcançados**

1. **100% de Cobertura Estrutural:** Todas as 8 entidades foram mapeadas com sucesso
2. **3 APIs Diferentes Identificadas:** GraphQL, Data Export, REST (Relatórios)
3. **Metodologia Validada:** Inspeção via DevTools (F12) provou ser eficaz
4. **Documentação Detalhada:** Cada entidade tem documentação completa com exemplos

### ⚠️ **Problema Crítico Identificado**

**API REST de Relatórios** (`/report/...`) requer autenticação de sessão (Cookie + CSRF Token), o que **impede automação programática** sem intervenção manual.

---

## 📋 **INVENTÁRIO COMPLETO DAS APIs**

### **1. API GraphQL** ✅ FUNCIONAL

| Entidade | Endpoint | Método | Autenticação | Status |
|----------|----------|--------|--------------|--------|
| **Fretes** | `POST /graphql` | POST | Bearer Token (`token_graphql`) | ✅ 100% |
| **Coletas** | `POST /graphql` | POST | Bearer Token (`token_graphql`) | ✅ 100% |

**Características:**
- ✅ Autenticação programática (Bearer Token)
- ✅ Endpoint único (`/graphql`)
- ✅ Queries tipadas e validadas
- ✅ Introspection disponível
- ✅ Paginação via `endCursor` e `hasNextPage`
- ⚠️ **Limitação:** Não possui `totalCount` (requer paginação completa)

**Cobertura de Dados:**
- Fretes: 100% (todos os campos do CSV mapeados)
- Coletas: 100% (todos os campos do CSV mapeados)

---

### **2. API Data Export** ✅ FUNCIONAL

| Entidade | Endpoint | Método | Autenticação | Status |
|----------|----------|--------|--------------|--------|
| **Cotações** | `GET /api/analytics/reports/6906/data` | GET | Bearer Token (`token_dataexport`) | ✅ 100% |
| **Manifestos** | `GET /api/analytics/reports/6399/data` | GET | Bearer Token (`token_dataexport`) | ✅ 100% |
| **Localizador de Cargas** | `GET /api/analytics/reports/8656/data` | GET | Bearer Token (`token_dataexport`) | ✅ 100% |

**Características:**
- ✅ Autenticação programática (Bearer Token)
- ✅ Endpoints padronizados (Template ID na URL)
- ✅ Schema plano (sem aninhamento complexo)
- ✅ Paginação via `page` e `per`
- ✅ Filtros via body JSON
- ✅ **Vantagem:** Dados já formatados para relatórios

**Cobertura de Dados:**
- Cotações: 100% (36 campos do CSV)
- Manifestos: 100% (80 campos do CSV)
- Localizador de Cargas: 100% (17 campos do CSV)

---

### **3. API REST (Relatórios)** ❌ PROBLEMA DE AUTENTICAÇÃO

| Entidade | Endpoint | Método | Autenticação | Status |
|----------|----------|--------|--------------|--------|
| **Faturas a Pagar** | `POST /report/accounting/debits/analytical` | POST | Cookie + CSRF Token | ⚠️ 100% dados, mas não programático |
| **Faturas a Receber** | `POST /report/accounting/credits/analytical` | POST | Cookie + CSRF Token | ⚠️ 100% dados, mas não programático |
| **Ocorrências** | `POST /report/invoice_occurrence/histories/analytical` | POST | Cookie + CSRF Token | ⚠️ 100% dados, mas não programático |

**Características:**
- ❌ **Autenticação de sessão** (Cookie `_tmsweb_session` + `X-CSRF-Token`)
- ❌ **Não programática** (requer login manual no navegador)
- ✅ Dados completos (24-30 campos por entidade)
- ✅ Schema plano com prefixo `report_*`
- ✅ Paginação via `page` e `per`
- ✅ Filtros via form-urlencoded

**Cobertura de Dados:**
- Faturas a Pagar: 100% (24 campos do CSV)
- Faturas a Receber: 100% (23 campos do CSV)
- Ocorrências: 100% (18 campos do CSV)

**Problema Crítico:**
- Cookie expira periodicamente (1-24 horas)
- CSRF Token muda a cada sessão
- **Impossível automatizar** sem login programático ou suporte da ESL

---

## 🔍 **ANÁLISE DETALHADA POR CATEGORIA**

### **A. Comparativo: API REST Tradicional vs API de Relatórios**

| Aspecto | API REST `/api/...` | API Relatórios `/report/...` |
|---------|---------------------|------------------------------|
| **Autenticação** | Bearer Token ✅ | Cookie + CSRF ❌ |
| **Método** | GET | POST |
| **Body** | Query params | Form URL Encoded |
| **Cobertura de Dados** | ~30-40% (incompleto) | 100% (completo) ✅ |
| **Uso** | Transações paginadas | Relatórios analíticos |
| **Programático** | ✅ Sim | ❌ Não (requer sessão) |

**Conclusão:** As APIs são **complementares**, mas a API de Relatórios é **essencial** para dados completos.

---

### **B. Padrões de Nomenclatura Identificados**

#### **GraphQL:**
- **Entidades:** `FreightBase`, `Pick`
- **Campos:** camelCase (`serviceAt`, `referenceNumber`)
- **Relacionamentos:** Objetos aninhados (`sender { mainAddress { city { name } } }`)

#### **Data Export:**
- **Campos:** snake_case com prefixos (`qoe_qes_sdr_nickname`, `mft_crn_psn_nickname`)
- **Estrutura:** Plana (sem aninhamento)
- **Padrão:** `{entidade}_{tipo}_{campo}`

#### **REST Relatórios:**
- **Campos:** snake_case com prefixo `report_*` (`report_receiver_cnpj`, `report_sequence_code`)
- **Estrutura:** Plana (sem aninhamento)
- **Padrão:** `report_{campo}`

**Observação:** A inconsistência de nomenclatura entre APIs dificulta a padronização do código.

---

### **C. Estrutura de Resposta**

#### **GraphQL:**
```json
{
  "data": {
    "freight": {
      "edges": [
        {
          "node": { ... }
        }
      ],
      "pageInfo": {
        "hasNextPage": true,
        "endCursor": "..."
      }
    }
  }
}
```

#### **Data Export:**
```json
[
  { "campo1": "valor1", ... },
  { "campo2": "valor2", ... }
]
```

#### **REST Relatórios:**
```json
{
  "collection": [
    { "report_campo1": "valor1", ... }
  ],
  "totals": {
    "report_count": 170
  }
}
```

**Observação:** Cada API tem estrutura diferente, exigindo parsers específicos.

---

## ⚠️ **PROBLEMAS E LIMITAÇÕES IDENTIFICADOS**

### **1. Problema Crítico: Autenticação REST Relatórios**

**Descrição:** Endpoints `/report/...` requerem Cookie de sessão + CSRF Token, obtidos apenas via login manual no navegador.

**Impacto:**
- ❌ Impossível automatizar via cron jobs
- ❌ Requer intervenção manual constante
- ❌ Cookie expira periodicamente

**Solução Proposta:**
- ✅ Documento técnico enviado ao suporte ESL
- ⏳ Aguardando resposta para liberar Bearer Token nos endpoints `/report/...`

---

### **2. Inconsistência de Autenticação**

**Problema:** 3 tipos de autenticação diferentes:
- GraphQL: `Bearer token_graphql`
- Data Export: `Bearer token_dataexport`
- REST Relatórios: `Cookie + X-CSRF-Token`

**Impacto:**
- Código mais complexo (3 estratégias diferentes)
- Manutenção mais difícil
- Configuração mais complexa

**Recomendação:** Padronizar autenticação (idealmente Bearer Token para todas).

---

### **3. Falta de Documentação Oficial**

**Problema:** Endpoints `/report/...` não estão documentados oficialmente.

**Impacto:**
- Descoberta via reverse engineering (DevTools)
- Sem garantia de estabilidade
- Sem documentação de parâmetros

**Recomendação:** Solicitar documentação oficial ao suporte ESL.

---

### **4. Paginação Inconsistente**

| API | Paginação | Limite |
|-----|-----------|--------|
| GraphQL | `endCursor` + `hasNextPage` | Sem `totalCount` |
| Data Export | `page` + `per` | Sem limite conhecido |
| REST Relatórios | `page` + `per` | Sem limite conhecido |

**Problema:** Cada API usa estratégia diferente de paginação.

**Impacto:**
- Código de paginação específico para cada API
- Dificuldade em estimar volume total

---

### **5. Validação de Volume Inconclusiva**

**Problema:** Alguns testes não validaram volume completo:
- Ocorrências: Retornou 1 registro (esperado ~4213) - **filtro de data incorreto**
- Fretes: Sem `totalCount` - **requer paginação completa**
- Coletas: Sem `totalCount` - **requer paginação completa**

**Impacto:**
- Incerteza sobre completude dos dados
- Necessidade de testes adicionais

**Recomendação:** Implementar validação de volume na extração completa.

---

## ✅ **PONTOS FORTES DA DOCUMENTAÇÃO**

### **1. Estrutura Consistente**

Todos os arquivos seguem o mesmo padrão:
- Objetivo
- Metodologia
- Configuração Insomnia
- Análise de Cobertura
- Conclusão

**Avaliação:** ⭐⭐⭐⭐⭐ Excelente

---

### **2. Mapeamento CSV vs API Detalhado**

Cada arquivo contém tabela completa de mapeamento:
- Coluna CSV (Origem)
- Chave API (Destino)
- Status (Mapeado/Não mapeado)

**Avaliação:** ⭐⭐⭐⭐⭐ Excelente

---

### **3. Exemplos Práticos**

Todos os arquivos incluem:
- Exemplos de requisição (Insomnia)
- Exemplos de resposta (JSON)
- Configuração completa

**Avaliação:** ⭐⭐⭐⭐⭐ Excelente

---

### **4. Metodologia Documentada**

Processo de descoberta bem documentado:
- Tentativas que falharam
- Correções aplicadas
- Introspection (GraphQL)
- DevTools (REST)

**Avaliação:** ⭐⭐⭐⭐⭐ Excelente

---

## 🔧 **CRÍTICAS CONSTRUTIVAS**

### **1. Arquivos Muito Grandes**

**Problema:** Alguns arquivos têm JSONs enormes (3000+ linhas).

**Exemplo:**
- `faturasapagar.md`: 3649 linhas
- `faturasareceber.md`: 3384 linhas
- `ocorrencias.md`: 3365 linhas

**Impacto:**
- Difícil navegação
- Lento para abrir/editar
- Maioria do conteúdo é JSON repetitivo

**Recomendação:**
- ✅ Manter apenas 2-3 exemplos de JSON
- ✅ Adicionar nota: "Exemplos completos disponíveis em [arquivo separado]"
- ✅ Ou truncar JSONs com `...` (mostrar início e fim)

---

### **2. Falta de Informações sobre Rate Limits**

**Problema:** Nenhum arquivo menciona:
- Rate limits da API
- Throttling
- Quotas de requisições

**Impacto:**
- Risco de bloqueio por excesso de requisições
- Sem estratégia de retry/backoff

**Recomendação:** Adicionar seção sobre rate limits (se conhecidos) ou nota sobre necessidade de investigação.

---

### **3. Falta de Informações sobre Erros**

**Problema:** Pouca documentação sobre:
- Códigos de erro possíveis
- Mensagens de erro comuns
- Como tratar erros

**Recomendação:** Adicionar seção "Tratamento de Erros" com exemplos.

---

### **4. Inconsistência em Validação de Volume**

**Problema:** Alguns arquivos validam volume, outros não:
- ✅ Faturas a Pagar: Validado (170 registros)
- ✅ Faturas a Receber: Validado (531 registros)
- ❌ Ocorrências: Falhou (1 vs 4213 esperados)
- ⚠️ GraphQL: Sem `totalCount` (inconclusivo)

**Recomendação:** Padronizar validação de volume ou documentar claramente quando não foi possível.

---

### **5. Falta de Informações sobre Filtros**

**Problema:** Documentação de filtros poderia ser mais detalhada:
- Quais filtros são obrigatórios?
- Quais filtros são opcionais?
- Formato de datas aceito?
- Valores possíveis para campos enum?

**Recomendação:** Adicionar seção "Filtros Disponíveis" com exemplos.

---

## 📊 **COMPARATIVO: APIs vs Necessidades**

### **Cobertura de Dados por API**

| Entidade | CSV (Campos) | GraphQL | Data Export | REST Relatórios |
|----------|--------------|---------|-------------|-----------------|
| Fretes | ~20 | ✅ 100% | ❌ N/A | ❌ N/A |
| Coletas | ~18 | ✅ 100% | ❌ N/A | ❌ N/A |
| Cotações | 36 | ❌ N/A | ✅ 100% | ❌ N/A |
| Manifestos | 80 | ❌ N/A | ✅ 100% | ❌ N/A |
| Localizador | 17 | ❌ N/A | ✅ 100% | ❌ N/A |
| Faturas Pagar | 24 | ❌ ~33% | ❌ N/A | ✅ 100% |
| Faturas Receber | 23 | ❌ ~30% | ❌ N/A | ✅ 100% |
| Ocorrências | 18 | ❌ ~17% | ❌ N/A | ✅ 100% |

**Conclusão:** Cada API cobre entidades específicas. **Não há redundância**, mas há **complementaridade**.

---

### **Viabilidade de Automação por API**

| API | Autenticação | Programático? | Status |
|-----|--------------|---------------|--------|
| GraphQL | Bearer Token | ✅ Sim | ✅ Pronto |
| Data Export | Bearer Token | ✅ Sim | ✅ Pronto |
| REST Relatórios | Cookie + CSRF | ❌ Não | ⏳ Aguardando suporte |

**Conclusão:** 5/8 entidades podem ser automatizadas **agora**. 3/8 dependem de solução do suporte ESL.

---

## 🎯 **RECOMENDAÇÕES ESTRATÉGICAS**

### **Curto Prazo (Imediato)**

1. ✅ **Implementar extração para 5 entidades funcionais:**
   - Fretes (GraphQL)
   - Coletas (GraphQL)
   - Cotações (Data Export)
   - Manifestos (Data Export)
   - Localizador de Cargas (Data Export)

2. ⏳ **Aguardar resposta do suporte ESL** sobre autenticação REST Relatórios

3. 📝 **Documentar rate limits** (investigar ou perguntar ao suporte)

---

### **Médio Prazo (Após Resposta do Suporte)**

1. **Se ESL liberar Bearer Token:**
   - ✅ Implementar extração para Faturas a Pagar, Faturas a Receber, Ocorrências
   - ✅ Usar autenticação Bearer Token (simples)

2. **Se ESL NÃO liberar Bearer Token:**
   - ⚠️ Implementar login programático em Java
   - ⚠️ Gerenciar renovação de sessão
   - ⚠️ Tratar expiração de Cookie

---

### **Longo Prazo (Melhorias)**

1. **Padronização:**
   - Criar abstração comum para as 3 APIs
   - Unificar tratamento de erros
   - Padronizar logging

2. **Otimização:**
   - Implementar cache de tokens
   - Implementar retry com backoff
   - Implementar circuit breaker

3. **Documentação:**
   - Criar README técnico consolidado
   - Documentar decisões arquiteturais
   - Criar guia de troubleshooting

---

## 📈 **MÉTRICAS DE QUALIDADE**

### **Cobertura de Dados**
- ✅ **100%** em todas as 8 entidades mapeadas

### **Documentação**
- ✅ **100%** das entidades documentadas
- ⭐⭐⭐⭐⭐ Qualidade: Excelente

### **Viabilidade de Automação**
- ✅ **62.5%** (5/8 entidades) prontas para automação
- ⏳ **37.5%** (3/8 entidades) aguardando solução de autenticação

### **Consistência**
- ⚠️ **Inconsistente** (3 tipos de autenticação, 3 estruturas de resposta)
- ⚠️ **Padronização necessária** no código

---

## 🏆 **CONCLUSÃO GERAL**

### **Pontos Fortes**

1. ✅ **Trabalho excepcional** de descoberta e mapeamento
2. ✅ **Documentação detalhada** e bem estruturada
3. ✅ **Metodologia validada** (DevTools + Introspection)
4. ✅ **100% de cobertura** estrutural em todas as entidades
5. ✅ **Identificação clara** do problema de autenticação

### **Pontos de Atenção**

1. ⚠️ **Autenticação REST Relatórios** bloqueia automação completa
2. ⚠️ **Inconsistência** entre APIs (nomenclatura, estrutura, paginação)
3. ⚠️ **Arquivos muito grandes** (JSONs repetitivos)
4. ⚠️ **Falta de informações** sobre rate limits e tratamento de erros

### **Avaliação Final**

**Nota Geral: 9/10** ⭐⭐⭐⭐⭐

**Justificativa:**
- Trabalho técnico excelente
- Documentação profissional
- Identificação precisa de problemas
- Pequenas melhorias necessárias (tamanho de arquivos, informações adicionais)

---

**Próximos Passos Recomendados:**
1. ✅ Implementar extração para 5 entidades funcionais
2. ⏳ Aguardar resposta do suporte ESL
3. 📝 Preparar implementação para REST Relatórios (com ou sem Bearer Token)

---

*Documento gerado em: 06/11/2025*  
*Versão: 1.0*

