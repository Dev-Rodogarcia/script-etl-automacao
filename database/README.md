# Scripts de Migração do Banco de Dados

Esta pasta contém todos os scripts SQL necessários para criar as tabelas e views do banco de dados em **produção**.

## 📋 Estrutura

### Tabelas (001-010)
- `001_criar_tabela_coletas.sql`
- `002_criar_tabela_fretes.sql`
- `003_criar_tabela_manifestos.sql`
- `004_criar_tabela_cotacoes.sql`
- `005_criar_tabela_localizacao_cargas.sql`
- `006_criar_tabela_contas_a_pagar.sql`
- `007_criar_tabela_faturas_por_cliente.sql`
- `008_criar_tabela_faturas_graphql.sql`
- `009_criar_tabela_log_extracoes.sql`
- `010_criar_tabela_page_audit.sql`

### Views Power BI Principais (011-018)
- `011_criar_view_faturas_por_cliente_powerbi.sql`
- `012_criar_view_fretes_powerbi.sql`
- `013_criar_view_coletas_powerbi.sql`
- `014_criar_view_faturas_graphql_powerbi.sql`
- `015_criar_view_cotacoes_powerbi.sql`
- `016_criar_view_contas_a_pagar_powerbi.sql`
- `017_criar_view_localizacao_cargas_powerbi.sql`
- `018_criar_view_manifestos_powerbi.sql`

### Views de Dimensões (019-023)
- `019_criar_view_dim_filiais.sql`
- `020_criar_view_dim_clientes.sql`
- `021_criar_view_dim_veiculos.sql`
- `022_criar_view_dim_motoristas.sql`
- `023_criar_view_dim_planocontas.sql`

### Configuração de Segurança (024)
- `024_configurar_permissoes_usuario.sql` - Configura permissões do usuário da aplicação (PRINCÍPIO DO MENOR PRIVILÉGIO)

### Validação (025)
- `025_validar_views_dimensao.sql` - Valida unicidade das chaves primárias nas views de dimensão (CRÍTICO para Power BI)

## 🚀 Como Executar

### Opção 1: Script Automático (Recomendado)

1. **Configure as variáveis de ambiente** antes de executar:
   
   **Para autenticação SQL Server:**
   ```cmd
   set DB_SERVER=servidor
   set DB_NAME=banco_de_dados
   set DB_USER=usuario
   set DB_PASSWORD=senha
   ```
   
   **Para autenticação integrada do Windows:**
   ```cmd
   set DB_SERVER=servidor
   set DB_NAME=banco_de_dados
   ```

2. Execute o script:
   ```cmd
   executar_database.bat
   ```

   **Nota:** As credenciais são lidas das variáveis de ambiente para maior segurança.

3. **APÓS executar todos os scripts**, configure as permissões do usuário:
   - Execute manualmente o script `024_configurar_permissoes_usuario.sql` no SSMS
   - Este script **não está incluído** no `executar_database.bat` por questões de segurança (requer permissões de administrador)

### Opção 2: SQL Server Management Studio (SSMS)

1. Abra o SQL Server Management Studio
2. Conecte-se ao banco de dados de produção
3. Execute os scripts na ordem numérica (001, 002, 003, ... 023)
4. **IMPORTANTE**: Execute primeiro as tabelas (001-010), depois as views principais (011-018) e por último as views de dimensões (019-023)

### Opção 3: sqlcmd (Linha de Comando)

```cmd
sqlcmd -S servidor -d banco -U usuario -P senha -i 001_criar_tabela_coletas.sql
sqlcmd -S servidor -d banco -U usuario -P senha -i 002_criar_tabela_fretes.sql
... (continue para todos os scripts)
```

## ⚠️ Importante

1. **Execute apenas UMA VEZ** no ambiente de produção
2. **Ordem de execução é crítica**: 
   - Primeiro as tabelas (001-010)
   - Depois as views principais (011-018)
   - Por último as views de dimensões (019-023)
   - **Finalmente**: Configure permissões (024) - PRINCÍPIO DO MENOR PRIVILÉGIO
   - **Recomendado**: Execute validação (025) - Garante unicidade das views de dimensão
3. **Backup**: Faça backup do banco antes de executar
4. **Teste**: Teste primeiro em ambiente de desenvolvimento/staging

## 🔒 Segurança: Permissões do Usuário da Aplicação

**IMPORTANTE**: Após criar as tabelas e views, configure as permissões do usuário da aplicação seguindo o **Princípio do Menor Privilégio**.

### Por que isso é importante?

Como a aplicação **não cria mais tabelas automaticamente**, o usuário do banco de dados configurado no `config.properties` (DB_USER) **não precisa e não deve ter permissões DDL** (CREATE, ALTER, DROP).

### Permissões Necessárias

O usuário da aplicação precisa apenas de:
- **SELECT** (leitura) - para consultas e validações
- **INSERT, UPDATE, DELETE** (escrita) - para operações MERGE (UPSERT)

**NÃO precisa de:**
- CREATE TABLE
- ALTER TABLE
- DROP TABLE
- ALTER SCHEMA
- CONTROL

### Como Configurar

1. Execute o script `024_configurar_permissoes_usuario.sql`
2. Substitua `usuario_aplicacao` pelo nome do usuário configurado no `config.properties` (DB_USER)
3. Substitua `seu_banco_de_dados` pelo nome do banco de dados
4. Descomente e execute a OPÇÃO 1 (recomendado) que usa roles padrão do SQL Server

### Benefício de Segurança

Se houver uma injeção de SQL, o atacante **não poderá destruir sua estrutura de dados**, pois o usuário da aplicação não tem permissões para criar, alterar ou excluir tabelas.

## ⚠️ VALIDAÇÃO CRÍTICA: Views de Dimensão

**IMPORTANTE**: Após criar as views de dimensão (019-023), **SEMPRE** execute o script `025_validar_views_dimensao.sql`.

### Por que isso é crítico?

As views de dimensão devem ter **chaves primárias únicas** para funcionar corretamente no Power BI com modelo Star Schema:

- **vw_dim_clientes**: Chave = `ID` (deve ser único)
- **vw_dim_filiais**: Chave = `NomeFilial` (deve ser único)
- **vw_dim_veiculos**: Chave = `Placa` (deve ser único)
- **vw_dim_motoristas**: Chave = `NomeMotorista` (deve ser único)
- **vw_dim_planocontas**: Chave = `Descricao` (deve ser único)

### O que acontece se houver duplicatas?

Se uma view dimensional retornar chaves duplicadas, o Power BI **não conseguirá criar relacionamentos** corretos (1:N) e gerará relacionamentos Muitos-para-Muitos não intencionais, quebrando o modelo Star Schema.

### Correções Aplicadas

As views foram corrigidas para garantir unicidade:

- **vw_dim_clientes**: Usa `GROUP BY ID` com `MAX(Nome)` para unificar IDs duplicados de múltiplas fontes
- **vw_dim_filiais**: Usa `UNION` com `LTRIM(RTRIM)` para normalizar nomes
- **vw_dim_veiculos**: Já usava `GROUP BY Placa` corretamente
- **vw_dim_motoristas**: Usa `DISTINCT` com normalização `UPPER + LTRIM + RTRIM`
- **vw_dim_planocontas**: Já usava `GROUP BY Descricao` corretamente

## 📝 Notas

- Todos os scripts usam `IF NOT EXISTS` para evitar erros se executados múltiplas vezes
- As views usam `CREATE OR ALTER` para permitir atualizações futuras
- Os scripts foram extraídos diretamente do código Java do projeto
- **Views de dimensão foram corrigidas para garantir unicidade das chaves primárias**