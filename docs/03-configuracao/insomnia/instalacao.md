# Guia de Instalação e Configuração do Insomnia

## 📥 Passo 1: Instalação do Insomnia

1. **Baixar Insomnia:**
   - Acesse: https://insomnia.rest/download
   - Escolha a versão para Windows
   - Baixe e instale (aceite as configurações padrão)

2. **Primeira Execução:**
   - Abra o Insomnia
   - Você pode pular o login (usar localmente)
   - Clique em "Skip" se aparecer opção de criar conta

---

## 📁 Passo 2: Criar Workspace

1. **Criar novo Workspace:**
   - No Insomnia, clique no botão "+" ou "Create"
   - Selecione **"Request Collection"**
   - Nome: `ESL Cloud API Testing`
   - Clique em "Create"

---

## 📂 Passo 3: Criar Estrutura de Pastas

Dentro do workspace criado, organize em 3 pastas principais:

### 3.1 Criar Pasta "API REST"
1. Clique com botão direito no workspace
2. Selecione "New Folder"
3. Nome: `API REST`

**Sub-pastas dentro de "API REST":**
- `Lançamentos a Pagar` (PRIORIDADE MÁXIMA)
- `Lançamentos a Receber`
- `Ocorrências`

### 3.2 Criar Pasta "API GraphQL"
1. Clique com botão direito no workspace
2. Selecione "New Folder"
3. Nome: `API GraphQL`

**Sub-pastas dentro de "API GraphQL":**
- `Coletas`
- `Fretes`

### 3.3 Criar Pasta "API Data Export"
1. Clique com botão direito no workspace
2. Selecione "New Folder"
3. Nome: `API Data Export`

**Sub-pastas dentro de "API Data Export":**
- `Manifestos`
- `Cotações`
- `Localizador de Cargas`

---

## 🔐 Passo 4: Configurar Environment

### 4.1 Obter os Tokens

**Localização dos tokens no projeto:**
- Arquivo: `config.properties` (na raiz do projeto Java)
- Ou variáveis de ambiente do Windows

**Tokens necessários:**
```properties
# Do config.properties:
api.rest.token=SEU_TOKEN_AQUI
api.graphql.token=SEU_TOKEN_AQUI  
api.dataexport.token=SEU_TOKEN_AQUI
```

### 4.2 Criar Environment no Insomnia

1. **Abrir Environment Manager:**
   - Clique no dropdown "No Environment" no topo
   - Selecione "Manage Environments"

2. **Criar Base Environment:**
   - Clique em "+" para criar novo environment
   - Nome: `ESL Cloud - Production`

3. **Adicionar Variáveis:**

Cole este JSON (substitua os tokens pelos valores reais do `config.properties`):

```json
{
  "base_url": "https://rodogarcia.eslcloud.com.br",
  "token_rest": "COLE_SEU_TOKEN_REST_AQUI",
  "token_graphql": "COLE_SEU_TOKEN_GRAPHQL_AQUI",
  "token_dataexport": "COLE_SEU_TOKEN_DATAEXPORT_AQUI",
  "data_inicio": "2025-11-02",
  "data_fim": "2025-11-03",
  "timestamp_inicio": "2025-11-02T00:00:00-03:00",
  "timestamp_fim": "2025-11-03T23:59:59-03:00"
}
```

4. **Salvar:**
   - Clique em "Done"
   - Selecione o environment criado no dropdown

---

## ✅ Verificação

Após concluir, você deve ter:

- ✅ Workspace: `ESL Cloud API Testing`
- ✅ 3 Pastas principais criadas
- ✅ Environment configurado com todos os tokens
- ✅ Environment ativo (selecionado no dropdown)

---

## 📌 Próximos Passos

Agora você está pronto para:
1. Importar as requisições de teste (próximo documento)
2. Começar os testes da API REST (Lançamentos a Pagar - PRIORIDADE)
3. Mapear todos os endpoints

**Documento seguinte:** `02-requisicoes-api-rest.md`

