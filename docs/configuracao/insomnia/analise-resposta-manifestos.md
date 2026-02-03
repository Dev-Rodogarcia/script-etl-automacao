# Análise da Resposta - Manifestos (Primeira Página)

## ✅ Requisição Testada

**Nome:** `[JSON] Manifestos - Página 1 (per=10000)`

**URL:** `{{base_url}}/api/analytics/reports/6399/data`

**Body:**
```json
{
  "search": {
    "manifests": {
      "service_date": "{{data_inicio}} - {{data_fim}}"
    }
  },
  "page": "1",
  "per": "10000"
}
```

## 📊 Informações da Resposta (Preencher)

### Status Code
- [ ] 200 OK
- [ ] Outro: _______

### Estrutura da Resposta
A resposta veio no formato:
- [ ] `{ "data": [...], "meta": {...} }` (com metadados de paginação)
- [ ] `[...]` (array direto)
- [ ] Outro formato: _______

### Quantidade de Registros
- Total retornado: _______ registros

### Informações de Paginação (se houver campo `meta`)
- `total_count`: _______ (total de registros disponíveis)
- `page`: _______ (página atual)
- `per`: _______ (registros por página)
- `total_pages`: _______ (se disponível)

### Verificação de Completude
- [ ] Todos os registros foram retornados (total_count = quantidade retornada)
- [ ] Há mais páginas disponíveis (total_count > quantidade retornada)
- [ ] Não é possível determinar (sem campo `meta`)

## 🔍 Análise de Campos

### Campos Presentes no Primeiro Registro
_(Listar todos os campos/chaves do primeiro objeto do array `data`)_

1. 
2. 
3. 
...

### Total de Campos por Registro
- _______ campos por registro

## 📝 Próximos Passos

Com base na análise:

1. **Se `total_count > quantidade retornada`:**
   - [ ] Criar requisição para página 2
   - [ ] Ajustar `per` se necessário

2. **Se todos os registros foram retornados:**
   - [ ] Validar campos retornados vs colunas desejadas
   - [ ] Prosseguir para próxima entidade (Cotações ou Localizador)

3. **Se houver problemas:**
   - [ ] Verificar status code
   - [ ] Verificar autenticação
   - [ ] Verificar formato do body

---

**Data do Teste:** _______
**Responsável:** _______

