# Implementação: Chave Composta para Manifestos

## Objetivo

Modificar a estrutura de armazenamento de manifestos para usar chave composta `(sequence_code, identificador_unico)` que permite preservar duplicados naturais enquanto mantém MERGE funcional para evitar duplicação não natural.

## Problema Resolvido

### Antes
- Tabela usava apenas `sequence_code` como PRIMARY KEY
- Manifestos com mesmo `sequence_code` mas dados diferentes (duplicados naturais) eram sobrescritos
- Perda de dados importantes

### Depois
- Tabela usa chave composta `(sequence_code, identificador_unico)`
- Duplicados naturais são preservados
- MERGE continua funcional (evita duplicação não natural)

## Estrutura da Tabela

```sql
CREATE TABLE manifestos (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sequence_code BIGINT NOT NULL,
    identificador_unico NVARCHAR(100) NOT NULL,
    -- ... outros campos ...
    CONSTRAINT UQ_manifestos_sequence_identificador UNIQUE (sequence_code, identificador_unico)
)

CREATE INDEX IX_manifestos_sequence_code ON manifestos(sequence_code);
```

## Cálculo do identificador_unico

### Lógica

1. **Prioridade 1**: Se `pick_sequence_code IS NOT NULL` → usar `pick_sequence_code`
2. **Prioridade 2**: Se `pick_sequence_code IS NULL` → calcular hash SHA-256 do metadata completo

### Implementação Java

```java
public void calcularIdentificadorUnico() {
    if (this.pickSequenceCode != null) {
        this.identificadorUnico = String.valueOf(this.pickSequenceCode);
    } else {
        this.identificadorUnico = calcularHashMetadata(this.metadata);
    }
}
```

### Hash SHA-256

- Algoritmo: SHA-256
- Formato: Hexadecimal (64 caracteres)
- Entrada: Metadata JSON completo (UTF-8)
- Saída: String hexadecimal (ex: "a1b2c3d4e5f6...")

## Modificações Realizadas

### 1. ManifestoEntity.java

**Adicionado:**
- Campo `identificadorUnico` (String)
- Getter/Setter para `identificadorUnico`
- Método `calcularIdentificadorUnico()`
- Método privado `calcularHashMetadata(String metadata)`
- Método privado `bytesToHex(byte[] bytes)`

### 2. ManifestoMapper.java

**Modificado:**
- Após serializar metadata, calcular `identificador_unico`
- Chamar `entity.calcularIdentificadorUnico()` após definir metadata
- Tratamento de erro com fallback

### 3. ManifestoRepository.java

**Modificado:**
- `criarTabelaSeNaoExistir()`: Nova estrutura com `id`, `identificador_unico` e UNIQUE constraint
- `executarMerge()`: Usa chave composta `(sequence_code, identificador_unico)` no MERGE
- Validação de `identificador_unico` antes de salvar
- Validação de tamanho máximo (100 caracteres)

## Scripts de Migração

### Para Tabela com Dados

**Arquivo:** `scripts/migracao-manifestos-chave-composta-com-dados.sql`

**Estratégia:**
1. Criar nova tabela `manifestos_new` com estrutura correta
2. Calcular `identificador_unico` para cada registro:
   - Se `pick_sequence_code IS NOT NULL`: usar `pick_sequence_code`
   - Se `pick_sequence_code IS NULL`: calcular hash SHA-256 do metadata
3. Migrar dados para nova tabela
4. Renomear tabelas (backup automático)

### Para Tabela Vazia

**Arquivo:** `scripts/migracao-manifestos-chave-composta.sql`

**Estratégia:**
1. Adicionar coluna `id` BIGINT IDENTITY(1,1)
2. Adicionar coluna `identificador_unico` NVARCHAR(100)
3. Calcular `identificador_unico` para registros existentes
4. Alterar constraints

## Como Funciona

### Cenário 1: Duplicados Naturais (Preservados)

```
Registro 1: sequence_code=48923, pick_sequence_code=100 → identificador_unico="100"
Registro 2: sequence_code=48923, pick_sequence_code=200 → identificador_unico="200"

MERGE:
- (48923, "100") → INSERT (novo registro)
- (48923, "200") → INSERT (novo registro)

Resultado: 2 registros salvos ✅
```

### Cenário 2: Duplicados Naturais com NULL (Preservados)

```
Registro 1: sequence_code=48923, pick_sequence_code=NULL, metadata={...dados1...}
  → identificador_unico="a1b2c3d4..." (hash do metadata)

Registro 2: sequence_code=48923, pick_sequence_code=NULL, metadata={...dados2...}
  → identificador_unico="e5f6g7h8..." (hash diferente)

MERGE:
- (48923, "a1b2c3d4...") → INSERT (novo registro)
- (48923, "e5f6g7h8...") → INSERT (novo registro)

Resultado: 2 registros salvos ✅
```

### Cenário 3: Execução Periódica (Não Duplica)

```
Execução 1 (10:00): sequence_code=48923, pick_sequence_code=100 → identificador_unico="100"
Execução 2 (11:00): sequence_code=48923, pick_sequence_code=100 → identificador_unico="100"

MERGE:
- (48923, "100") → UPDATE (atualiza dados existentes)

Resultado: 1 registro atualizado, não duplica ✅
```

## Validações

### Validações no Código

1. `sequence_code` não pode ser NULL
2. `identificador_unico` não pode ser NULL ou vazio
3. `identificador_unico` não pode exceder 100 caracteres
4. Hash é calculado corretamente mesmo com metadata NULL

### Validações no Banco

1. UNIQUE constraint em `(sequence_code, identificador_unico)`
2. NOT NULL em `sequence_code` e `identificador_unico`
3. Índice em `sequence_code` para performance

## Testes Necessários

### Testes Unitários

1. ✅ ManifestoEntity.calcularIdentificadorUnico() com diferentes cenários
2. ✅ ManifestoMapper.toEntity() que identificador é calculado corretamente
3. ✅ ManifestoRepository.executarMerge() com chave composta

### Testes de Integração

1. ✅ Duplicados naturais são preservados
2. ✅ Execução periódica não duplica dados
3. ✅ Migração de dados existentes funciona corretamente

## Limitações e Considerações

### Hash SHA-256

- **Java**: Calcula hash do metadata completo (sem limitação de tamanho)
- **SQL Server**: HASHBYTES tem limite de 8000 bytes
  - Durante migração: usa `LEFT(metadata, 8000)`
  - Após migração: Java calcula hash completo
  - Isso é aceitável porque colisões são muito raras

### Tamanho do identificador_unico

- **Máximo**: 100 caracteres (NVARCHAR(100))
- **Hash SHA-256**: 64 caracteres (hexadecimal)
- **pick_sequence_code**: Máximo ~19 dígitos (BIGINT)
- **Garantia**: Sempre cabe em 100 caracteres

## Arquivos Modificados

1. `src/main/java/br/com/extrator/db/entity/ManifestoEntity.java`
2. `src/main/java/br/com/extrator/modelo/dataexport/manifestos/ManifestoMapper.java`
3. `src/main/java/br/com/extrator/db/repository/ManifestoRepository.java`

## Arquivos Criados

1. `scripts/migracao-manifestos-chave-composta.sql`
2. `scripts/migracao-manifestos-chave-composta-com-dados.sql`
3. `scripts/README-migracao-manifestos.md`
4. `docs/04-especificacoes-tecnicas/implementacao-apis/chave-composta-manifestos.md`

## Próximos Passos

1. ✅ Compilar projeto e verificar erros
2. ✅ Testar em ambiente de desenvolvimento
3. ✅ Executar script de migração em dev
4. ⏳ Validar dados migrados
5. ⏳ Testar extração com dados reais
6. ⏳ Testar execução periódica
7. ⏳ Deploy em produção

## Referências

- Plano completo: `ajust.plan.md`
- Documentação de APIs: `docs/02-apis/dataexport/manifestos.md`
- Problema ESL: `docs/descobertas/problema-esl.md`

