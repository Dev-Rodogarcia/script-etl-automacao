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
# 📘 Guia Definitivo: API Data Export da ESL Cloud

## 🎯 Objetivo

Este guia detalha o processo completo para **extrair dados de relatórios (templates)** da **API Data Export da ESL Cloud**.
Ele cobre os dois fluxos de trabalho principais:

* **Consulta síncrona** (para receber dados em formato JSON)
* **Consulta assíncrona** (para solicitar a geração e o download de arquivos)

---

## 🧩 1. Pré-requisitos Essenciais

Antes de iniciar, certifique-se de ter as seguintes informações:

* **Tenant:** O subdomínio da sua empresa.
  Para este projeto: `rodogarcia1`

* **Token de Autenticação:** Um *Bearer Token* gerado para o usuário da API.
  **Token utilizado:**

  ```
  ***TOKEN_CENSURADO_POR_SEGURANCA***
  ```

* **URL Base:**

  ```
  https://[tenant].eslcloud.com.br
  ```

* **Política de Rate Limit:**
  Todas as requisições devem respeitar um **intervalo mínimo de 2 segundos** entre elas para o mesmo endereço IP.
  Caso contrário, a API retornará o erro `429 Too Many Requests`.

---

## ⚙️ 2. O Fluxo de Trabalho Completo (Passo a Passo)

O processo de extração segue uma sequência lógica de requisições.

---

### 🧾 **Passo 1: Consulta todos os templates**

**Objetivo:** Obter a lista de relatórios (templates) disponíveis e seus respectivos IDs.
**Método:** `GET`
**URL:**

```
https://rodogarcia.eslcloud.com.br/api/analytics/reports?per=100
```

> ⚠️ Use o parâmetro `?per=100` para garantir que todos os templates sejam retornados em uma única chamada, evitando paginação.

**Corpo (Body):** Nenhum.
**Resposta Esperada:** Um array JSON contendo uma lista de objetos (cada objeto representa um template).

**Exemplo de Resposta (parcial):**

```json
[
  {
    "id": 6906,
    "name": "Relação de Cotações Detalhada",
    "root": "quote"
  },
  {
    "id": 6399,
    "name": "Relação de Manifestos Detalhada",
    "root": "manifest"
  }
]
```

---

### 🧠 **Passo 2: Consulta estrutura dos templates**

**Objetivo:** Identificar a `table` e o `field` necessários para montar o corpo da requisição de consulta.
**Método:** `GET`
**URL:**

```
https://rodogarcia.eslcloud.com.br/api/analytics/reports/{id}/info
```

> Substitua `{id}` pelo ID obtido no Passo 1 (ex: `6399`).

**Corpo (Body):** Nenhum.
**Resposta Esperada:** Um objeto JSON detalhando os campos e filtros do template.

**Principais seções da resposta:**

* `filters`: lista de filtros disponíveis (de onde extraímos `table` e `field`)
* `required_date_filters`: confirma qual é o campo de data obrigatório

**Exemplo de Resposta (parcial para Manifestos):**

```json
{
  "filters": [
    {
      "label": "Data",
      "field": "service_date",
      "table": "manifests",
      "type": "date"
    }
  ],
  "required_date_filters": [
    "manifests.service_date"
  ]
}
```

---

### 📤 **Passo 3: Extração dos Dados**

Após identificar os filtros, existem **dois fluxos possíveis** para extrair os dados:

---

## 🔹 Fluxo A: Consulta Síncrona (JSON)

**Objetivo:** Obter os dados do relatório diretamente na resposta da API em formato JSON.
**Método:** `GET`
**URL:**

```
https://rodogarcia.eslcloud.com.br/api/analytics/reports/{id}/data
```

> Substitua `{id}` pelo ID do template (ex: `6399`).

**Corpo (Body):**
Embora seja uma requisição `GET`, **os filtros devem ser enviados no corpo** da requisição em formato JSON (*raw* no Postman).
Enviar filtros na URL causará erro `422`.

**Estrutura do Corpo:**

```json
{
  "search": {
    "[table]": {
      "[field]": "YYYY-MM-DD - YYYY-MM-DD"
    }
  }
}
```

> Substitua `[table]` e `[field]` pelos valores obtidos no Passo 2 e forneça um intervalo de datas válido.

**Resposta Esperada:**
Um array JSON com os dados do relatório para o período solicitado.

---

## 🔹 Fluxo B: Consulta Assíncrona (Geração de Arquivo)

**Objetivo:** Solicitar que a ESL gere um arquivo (XLSX) em segundo plano, ideal para grandes volumes de dados.

Este fluxo é composto por **duas requisições**.

---

### 📥 **POST 4) Solicitação de exportação**

**Método:** `POST`
**URL:**

```
https://rodogarcia.eslcloud.com.br/api/analytics/reports/{id}/export
```

> Atenção: a URL termina com `/export`. Usar outra rota causará erro `404`.

**Corpo (Body):**

```json
{
  "export_to_ftp": false,
  "search": {
    "[table]": {
      "[field]": "YYYY-MM-DD - YYYY-MM-DD"
    }
  }
}
```

**Resposta Esperada:**

```json
{
  "status": "enqueued",
  "id": 12345
}
```

> Guarde o `id` retornado — ele é o **ID do arquivo gerado**.

---

### 📦 **GET 5) Consulta do arquivo gerado**

**Objetivo:** Verificar o status da geração do arquivo e obter o link de download.
**Método:** `GET`
**URL:**

```
https://rodogarcia.eslcloud.com.br/api/analytics/report_files/{id_do_arquivo}
```

> Substitua `{id_do_arquivo}` pelo `id` retornado no passo anterior.

**Corpo (Body):** Nenhum.
**Processo:** Faça esta chamada periodicamente (respeitando o *rate limit*) até que o campo `status` mude para `"generated"`.

**Resposta Final Esperada:**

```json
{
  "status": "generated",
  "download_url": "https://rodogarcia.eslcloud.com.br/downloads/arquivo.xlsx"
}
```

---

## 🧭 3. Referência Rápida: Templates Validados

| Nome do Relatório               | ID do Template | Table de Filtro | Field de Filtro de Data |
| ------------------------------- | -------------- | --------------- | ----------------------- |
| Relação de Cotações Detalhada   | 6906           | quotes          | requested_at            |
| Relação de Coletas Detalhada    | 6908           | picks           | request_date            |
| Relação de Manifestos Detalhada | 6399           | manifests       | service_date            |
| Localizador de Cargas           | 8656           | freights        | service_at              |
