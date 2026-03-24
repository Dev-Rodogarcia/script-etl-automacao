---
context:
  - Documentacao
  - Legado
  - ArquivoHistorico
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: docs
classification: legado
related_files:
  - docs/index.md
  - docs/legado/index.md
  - docs/legado/classificacao.md
---
# 📚 Guias de Teste com Insomnia - ESL Cloud APIs

Este diretório contém todos os guias necessários para mapear completamente as APIs da ESL Cloud usando o Insomnia.

---

## 🎯 Objetivo Geral

Descobrir os endpoints e parâmetros corretos que retornam **100% dos dados** dos 8 relatórios manuais:

1. ✅ Lançamentos a Pagar (PRIORIDADE MÁXIMA)
2. ✅ Lançamentos a Receber  
3. ✅ Ocorrências
4. ✅ Coletas
5. ✅ Fretes
6. ✅ Manifestos
7. ✅ Cotações
8. ✅ Localizador de Cargas

---

## 📖 Ordem de Leitura dos Guias

### Fase 1: Setup Inicial (FAZER PRIMEIRO)

1. **[01-instalacao-configuracao.md](01-instalacao-configuracao.md)**
   - Instalar Insomnia
   - Criar workspace
   - Configurar pastas
   - Setup do environment

2. **[05-como-obter-tokens.md](05-como-obter-tokens.md)**
   - Localizar tokens no config.properties
   - Copiar para environment
   - Validar autenticação

### Fase 2: Guia Rápido (COMEÇAR AQUI DEPOIS DO SETUP)

3. **[06-guia-rapido-testes.md](06-guia-rapido-testes.md)**
   - Primeiro teste em 5 minutos
   - Checklist de validação
   - Troubleshooting comum

### Fase 3: Testes Detalhados (EXECUTAR NA ORDEM)

4. **[02-requisicoes-api-rest.md](02-requisicoes-api-rest.md)**
   - API REST - Lançamentos a Pagar (PRIORIDADE)
   - API REST - Lançamentos a Receber
   - API REST - Ocorrências

5. **[03-requisicoes-api-graphql.md](03-requisicoes-api-graphql.md)**
   - GraphQL Introspection
   - Coletas (Pick)
   - Fretes (Freight)

6. **[04-requisicoes-api-dataexport.md](04-requisicoes-api-dataexport.md)**
   - Manifestos
   - Cotações
   - Localizador de Cargas

---

## 🗂️ Estrutura do Workspace Insomnia

```
ESL Cloud API Testing/
├── API REST/
│   ├── Lançamentos a Pagar/         (🔥 PRIORIDADE MÁXIMA)
│   │   ├── [ATUAL] GET billings
│   │   ├── [TESTE 1] GET entries
│   │   ├── [TESTE 2] GET transactions
│   │   └── ...
│   ├── Lançamentos a Receber/
│   └── Ocorrências/
│
├── API GraphQL/
│   ├── Introspection/
│   │   ├── Listar Types
│   │   ├── Campos Pick
│   │   └── Campos Freight
│   ├── Coletas/
│   └── Fretes/
│
└── API Data Export/
    ├── Manifestos/
    ├── Cotações/
    └── Localizador de Cargas/
```

---

## 🔐 Environment Configurado

```json
{
  "base_url": "https://rodogarcia.eslcloud.com.br",
  "token_rest": "...",
  "token_graphql": "...",
  "token_dataexport": "...",
  "data_inicio": "2025-11-02",
  "data_fim": "2025-11-03",
  "timestamp_inicio": "2025-11-02T00:00:00-03:00",
  "timestamp_fim": "2025-11-03T23:59:59-03:00"
}
```

**Template:** [insomnia-environment-template.json](insomnia-environment-template.json)

---

## ✅ Checklist de Progresso

### Setup Inicial

- [ ] Insomnia instalado
- [ ] Workspace criado: "ESL Cloud API Testing"
- [ ] 3 pastas principais criadas (REST, GraphQL, Data Export)
- [ ] Environment configurado com tokens
- [ ] Teste rápido executado com sucesso (status 200)

### API REST

- [ ] Lançamentos a Pagar: Endpoint correto descoberto
- [ ] Lançamentos a Pagar: Evidências validadas (Taxas Banco + Vales)
- [ ] Lançamentos a Receber: Endpoint validado (533 linhas)
- [ ] Ocorrências: Endpoint validado (4213 linhas)

### API GraphQL

- [ ] Introspection executada para Pick
- [ ] Introspection executada para Freight
- [ ] Lista completa de campos documentada
- [ ] Campos faltantes identificados
- [ ] Query expandida testada

### API Data Export

- [ ] XLSX convertidos para CSV
- [ ] Manifestos: Campos validados
- [ ] Cotações: Campos validados
- [ ] Localizador: Campos validados

### Documentação

- [ ] Descobertas documentadas em `docs/descobertas/`
- [ ] Mapeamentos criados em `docs/mapeamento/`
- [ ] Endpoints documentados em `docs/endpoints/`
- [ ] Planilha consolidada criada

---

## 📊 Critérios de Sucesso

Para cada entidade testada:

| Critério | Meta | Como Validar |
|----------|------|--------------|
| Status HTTP | 200 OK | Verde no Insomnia |
| Total de Registros | ≥90% do CSV | Comparar `totalCount` |
| Campos Mapeados | ≥95% | Tabela de mapeamento |
| Evidências Encontradas | 100% | Ctrl+F valores específicos |

---

## 🚨 Problema Crítico Prioritário

**Lançamentos a Pagar - Incompletude Confirmada:**

- ❌ Endpoint atual: `/api/accounting/debit/billings`
- ❌ Retorna apenas: CiotBilling, DriverBilling
- ❌ CSV manual tem: Taxas de Banco, Vales, Faturas

**Evidências que DEVEM ser encontradas:**
1. "BANCO BRADESCO S.A" (CNPJ: 60746948002590)
2. "JOÃO DA SILVA" (Vale de R$ 275,00)
3. "MTZ - RODOGARCIA" (empresa emissora de vales)

**Ação:** Testar TODOS os endpoints alternativos até encontrar o correto!

---

## 📂 Diretórios Relacionados

- **Descobertas:** `../descobertas/` - Resultados dos testes
- **Mapeamentos:** `../mapeamento/` - Tabelas CSV ↔ API
- **Endpoints:** `../endpoints/` - Documentação final dos endpoints
- **CSVs Originais:** `../arquivos-csv/` - Relatórios de referência

---

## 💡 Dicas Importantes

### Performance
- Começar com `per=10` para testes rápidos
- Aumentar para `per=100` após confirmar que funciona
- Usar `per=10000` apenas para CSV completo

### Organização
- Nomear requests claramente: `[TESTE X]`, `[SUCESSO]`, `[FALHOU]`
- Usar descrições nas requests para anotar descobertas
- Duplicar requests antes de modificar

### Validação
- Sempre procurar evidências específicas do CSV
- Não confiar apenas no status 200
- Verificar se `data` array não está vazio
- Comparar total de registros

---

## 🆘 Se Precisar de Ajuda

1. **Consultar documentação oficial:**
   - REST: https://documenter.getpostman.com/view/20571375/2s9YXk2fj5
   - GraphQL: https://implantacao.eslcloud.com.br/graphql_docs

2. **Verificar guias detalhados** neste diretório

3. **Inspecionar Network** da plataforma web (F12)

4. **Contactar suporte ESL** usando template em:
   - `../relatorios-diarios/pedido-endpoints.md`

---

## 🎯 Meta Final

Após completar TODOS os testes e mapeamentos:

✅ **Código Java atualizado** com endpoints corretos  
✅ **DTOs expandidos** com todos os campos  
✅ **100% dos dados** sendo extraídos  
✅ **Documentação completa** para manutenção futura  

**Resultado:** Script de automação tão completo quanto a extração manual! 🚀

---

**Começar por:** [01-instalacao-configuracao.md](01-instalacao-configuracao.md)

