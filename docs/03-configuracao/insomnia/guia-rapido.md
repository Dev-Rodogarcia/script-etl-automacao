# 🚀 Guia Rápido de Testes no Insomnia

Este é um guia express para você começar a testar AGORA as APIs da ESL Cloud.

---

## ⏱️ 5 Minutos para o Primeiro Teste

### 1️⃣ Abrir Insomnia
- Workspace: `ESL Cloud API Testing`
- Environment: `ESL Cloud - Production` (verificar se está selecionado)

### 2️⃣ Criar Request de Teste Rápido

**Pasta:** API REST > Lançamentos a Pagar

**Nome:** `[TESTE RÁPIDO] Endpoint Atual`

**Configuração:**
- Método: **GET**
- URL: `{{base_url}}/api/accounting/debit/billings?since={{timestamp_inicio}}&per=10`
- Headers:
  ```
  Authorization: Bearer {{token_rest}}
  Accept: application/json
  ```

### 3️⃣ Executar (Botão "Send")

### 4️⃣ Analisar Resposta

**Se receber 200 OK:**
```json
{
  "data": [
    {
      "id": 123,
      "type": "CiotBilling",
      ...
    }
  ],
  "paging": {
    "next_id": "456"
  }
}
```

✅ **Token está funcionando!**

**Se receber 401 Unauthorized:**
```json
{
  "error": "Unauthorized"
}
```

❌ **Token inválido** - Verificar config.properties

**Se receber 404 Not Found:**
```json
{
  "error": "Not Found"
}
```

⚠️ **Endpoint incorreto** - Tentar outros endpoints

---

## 🎯 Checklist Rápida de Validação

Para cada endpoint testado, verificar:

- [ ] Status 200 OK?
- [ ] JSON bem formatado?
- [ ] Campo `data` existe?
- [ ] Array `data` NÃO está vazio?
- [ ] Tem campos que você reconhece do CSV?

Se TODOS ✅, prosseguir com testes mais detalhados!

---

## 🔍 O Que Procurar nas Respostas

### Lançamentos a Pagar - EVIDÊNCIAS CRÍTICAS

Procurar na resposta JSON (Ctrl+F):

**1. Taxa de Banco:**
```
"BANCO BRADESCO"
```
ou
```
"60746948002590"
```

**2. Vales de Funcionários:**
```
"JOÃO DA SILVA"
```
ou
```
"VALE ALIMENTAÇÃO"
```

**3. Valores Específicos:**
```
275
582
15.12
```

Se encontrar QUALQUER uma destas evidências → **ENDPOINT CORRETO!** 🎉

---

## 📊 Comparação Rápida de Volumes

| Entidade | CSV Manual | Verificar na API |
|----------|------------|------------------|
| Lançamentos a Pagar | 74 linhas | `paging.total` ou contar |
| Lançamentos a Receber | 533 linhas | Idem |
| Ocorrências | 4213 linhas | Idem |
| Coletas | 476 linhas | `totalCount` |
| Fretes | ~400 linhas | `totalCount` |

**Tolerância:** ±10% é aceitável (dados podem ter mudado entre downloads)

**Crítico:** Se API retorna <50% do CSV → endpoint está incompleto!

---

## 🔄 Fluxo de Teste Recomendado

### Para API REST:

```
1. Testar endpoint atual
   ↓
2. Verificar evidências (Ctrl+F no JSON)
   ↓
3. Se NÃO encontrou evidências → Testar próximo endpoint
   ↓
4. Repetir até encontrar endpoint completo
   ↓
5. Documentar descoberta
```

### Para API GraphQL:

```
1. Executar Introspection
   ↓
2. Copiar lista COMPLETA de campos
   ↓
3. Comparar com campos atuais do código
   ↓
4. Identificar campos faltantes
   ↓
5. Adicionar à query
   ↓
6. Testar query expandida
   ↓
7. Documentar
```

### Para API Data Export:

```
1. Request JSON (per=100)
   ↓
2. Contar campos retornados
   ↓
3. Request CSV (per=10000)
   ↓
4. Salvar resposta como .csv
   ↓
5. Comparar colunas com XLSX original
   ↓
6. Documentar diferenças
```

---

## 💡 Dicas Importantes

### Atalhos Úteis no Insomnia

- `Ctrl + E` : Abrir Manage Environments
- `Ctrl + Enter` : Executar request
- `Ctrl + F` : Procurar na resposta
- `Ctrl + S` : Salvar request

### Organização

**Nomear requests claramente:**
```
✅ BOM: "[TESTE 1] GET debit/entries"
✅ BOM: "[SUCESSO] GET payables - Retorna Tudo"
❌ RUIM: "Request 1"
❌ RUIM: "teste"
```

**Usar cores/tags:**
- 🟢 Verde: Endpoints que funcionam
- 🔴 Vermelho: Endpoints que falharam
- 🟡 Amarelo: Endpoints parciais

### Salvar Evidências

Quando encontrar um endpoint que funciona:

1. Copiar URL completa
2. Copiar headers
3. Salvar um exemplo de resposta
4. Fazer screenshot se necessário
5. Documentar imediatamente

---

## 🚨 Troubleshooting Rápido

### Erro 401 Unauthorized
- Token expirado/inválido
- Verificar `{{token_rest}}` no environment
- Obter novo token do config.properties

### Erro 404 Not Found
- URL incorreta
- Endpoint não existe
- Testar outro endpoint da lista

### Erro 403 Forbidden
- Token sem permissões
- Contactar admin da plataforma
- Solicitar permissões de leitura

### Response vazio `"data": []`
- Filtro de data muito restrito
- Testar com intervalo maior
- Verificar se campo de data está correto

### Request demora muito (timeout)
- Reduzir `per` (ex: de 100 para 10)
- Verificar conexão internet
- Aumentar timeout no Insomnia (Settings)

---

## ✅ Quando um Teste é Bem-Sucedido

Você saberá que o endpoint está correto quando:

1. ✅ Status 200 OK
2. ✅ JSON válido com `data` array
3. ✅ Total de registros próximo do CSV (±10%)
4. ✅ Encontrou evidências específicas (nomes, CNPJs, valores)
5. ✅ Campos retornados cobrem maioria do CSV

**Ação:** Documentar IMEDIATAMENTE em `docs/descobertas/`

---

## 📌 Próximos Passos

Após completar um teste bem-sucedido:

1. ✅ Marcar to-do como completo
2. ✅ Documentar em `docs/descobertas/`
3. ✅ Criar mapeamento em `docs/mapeamento/`
4. ✅ Partir para próxima entidade

**Meta:** Completar TODAS as 8 entidades!

---

Boa sorte! 🚀 Qualquer dúvida, consulte os guias detalhados em `docs/insomnia/`.

