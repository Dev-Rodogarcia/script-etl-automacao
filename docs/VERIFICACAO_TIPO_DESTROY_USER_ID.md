# ⚠️ VERIFICAÇÃO NECESSÁRIA: Tipo de destroyUserId e cancellationUserId

## Problema Identificado

A implementação atual assume que `destroyUserId` e `cancellationUserId` na tabela `coletas` referem-se ao tipo `Individual` (usuários do sistema), mas **NÃO HÁ GARANTIA** de que isso está correto.

## Campos na Query GraphQL de Coletas

1. **`user { id name }`** - Objeto relacionado (linha 51 da query)
   - Tipo: Provavelmente `User` ou `Individual`
   - Usado para: Usuário/Motorista que criou a coleta

2. **`destroyUserId`** - Campo escalar Long (linha 62)
   - Tipo: **DESCONHECIDO** - Pode ser `Individual`, `User`, `Person`, ou outro tipo
   - Usado para: ID do usuário que excluiu a coleta

3. **`cancellationUserId`** - Campo escalar Long (linha 58)
   - Tipo: **DESCONHECIDO** - Pode ser `Individual`, `User`, `Person`, ou outro tipo
   - Usado para: ID do usuário que cancelou a coleta

## Risco

Se `destroyUserId` e `cancellationUserId` forem de um tipo diferente de `Individual`:
- ❌ Cruzamento com `dim_usuarios` retornará NULL ou dados incorretos
- ❌ Pode misturar IDs de motoristas, agentes, ou outras entidades com usuários do sistema

## Solução Recomendada

### Opção 1: Verificação via Introspecção GraphQL (RECOMENDADO)

Executar uma query de introspecção para descobrir o tipo exato:

```graphql
query IntrospectPickType {
  __type(name: "Pick") {
    fields {
      name
      type {
        name
        kind
        ofType {
          name
          kind
        }
      }
    }
  }
}
```

Filtrar por `destroyUserId` e `cancellationUserId` para ver o tipo retornado.

### Opção 2: Validação Cruzada com Dados Reais

1. Extrair algumas coletas com `destroyUserId` preenchido
2. Verificar se esses IDs existem na tabela `dim_usuarios` (Individual)
3. Se muitos IDs não forem encontrados, provavelmente são de outro tipo

### Opção 3: Consultar Documentação da API

Verificar se há documentação oficial sobre o tipo desses campos.

## Implementação Atual

A implementação atual usa:
- Query: `individual(params: {enabled: true})` → Tipo `Individual`
- Tabela: `dim_usuarios` com `user_id` (bigint)
- JOIN: `c.destroy_user_id = u.user_id`

**⚠️ ATENÇÃO:** Se `destroyUserId` não for do tipo `Individual`, o JOIN não funcionará corretamente.

## Próximos Passos

1. ✅ Executar introspecção GraphQL para confirmar o tipo
2. ✅ Validar com dados reais se os IDs batem
3. ✅ Ajustar implementação se necessário
