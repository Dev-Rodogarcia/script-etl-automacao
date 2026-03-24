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
# Como Obter os Tokens de Autenticação

## 📍 Localização dos Tokens no Projeto

### Opção 1: Arquivo config.properties (RECOMENDADO)

**Localização:**
```
C:\Users\lucas\OneDrive\Área de Trabalho\Projetos\ESTAGIO\script-automacao\config.properties
```

**Abrir com:** Bloco de Notas ou Visual Studio Code

**Procurar por estas linhas:**
```properties
# Token para API REST
api.rest.token=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

# Token para API GraphQL  
api.graphql.token=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

# Token para API Data Export
api.dataexport.token=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

**Copiar:** O valor APÓS o sinal de `=`

---

### Opção 2: Variáveis de Ambiente do Windows

Se os tokens NÃO estiverem no config.properties, verificar variáveis de ambiente:

1. **Abrir Variáveis de Ambiente:**
   - Pressionar `Win + R`
   - Digite: `sysdm.cpl`
   - Aba "Avançado" → "Variáveis de Ambiente"

2. **Procurar por:**
   - `API_REST_TOKEN`
   - `API_GRAPHQL_TOKEN`
   - `API_DATAEXPORT_TOKEN`

3. **Copiar os valores**

---

### Opção 3: Arquivo .env (Se Existir)

**Localização:**
```
C:\Users\lucas\OneDrive\Área de Trabalho\Projetos\ESTAGIO\script-automacao\.env
```

**Nota:** Este arquivo está em .cursorignore, então pode não estar visível no IDE

**Procurar por:**
```env
API_REST_TOKEN=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
API_GRAPHQL_TOKEN=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
API_DATAEXPORT_TOKEN=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

---

## 🔐 Formato dos Tokens

Os tokens da ESL Cloud geralmente são:
- **Alfanuméricos** (letras + números)
- **Longos** (~40-60 caracteres)
- **Case-sensitive** (maiúsculas/minúsculas importam)

Exemplo de formato:
```
abc123def456ghi789jkl012mno345pqr678stu901vwx234
```

---

## ✅ Validar se os Tokens Estão Corretos

### No próprio Insomnia (após configurar):

1. **Criar request de teste:**
   ```
   GET {{base_url}}/api/accounting/credit/billings?per=1
   Headers: Authorization: Bearer {{token_rest}}
   ```

2. **Executar (Send)**

3. **Verificar resposta:**
   - ✅ **200 OK:** Token válido!
   - ❌ **401 Unauthorized:** Token inválido ou expirado
   - ❌ **403 Forbidden:** Token sem permissões

### Via Command Line (Windows PowerShell):

```powershell
# Testar token REST
$token = "SEU_TOKEN_AQUI"
$headers = @{
    "Authorization" = "Bearer $token"
    "Accept" = "application/json"
}
Invoke-WebRequest -Uri "https://rodogarcia.eslcloud.com.br/api/accounting/credit/billings?per=1" -Headers $headers
```

---

## ⚠️ Segurança dos Tokens

**IMPORTANTE:**
- ❌ NÃO compartilhar tokens publicamente
- ❌ NÃO commitar tokens no Git
- ❌ NÃO incluir em screenshots
- ✅ Manter tokens APENAS localmente
- ✅ Usar variáveis de ambiente no Insomnia

---

## 🔄 Se os Tokens Expiraram

Caso receba erro 401 Unauthorized:

1. **Contactar administrador da plataforma ESL**
2. **Solicitar novos tokens** com permissões de leitura para:
   - API REST (accounting/debit, accounting/credit, occurrences)
   - API GraphQL (pick, freight)
   - API Data Export (analytics/reports)

3. **Atualizar tokens:**
   - No `config.properties`
   - No environment do Insomnia

---

## 📝 Checklist

Antes de prosseguir com os testes:

- [ ] Localizei o arquivo config.properties
- [ ] Copiei os 3 tokens (REST, GraphQL, Data Export)
- [ ] Colei no environment do Insomnia
- [ ] Testei um endpoint simples para validar
- [ ] Recebi resposta 200 OK (tokens válidos)

Se TODAS as etapas estão ✅, prossiga para os testes!

