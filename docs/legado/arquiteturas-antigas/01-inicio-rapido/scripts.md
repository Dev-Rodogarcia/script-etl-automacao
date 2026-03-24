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
# 📜 Guia de Scripts - ESL Cloud Extrator

## 🚀 Scripts Disponíveis

### 1️⃣ Compilação
```bash
05-compilar_projeto.bat
```
**Função:** Compila o projeto e gera o JAR  
**Quando usar:** Após mudanças no código  
**Resultado:** `target/extrator.jar`

---

### 2️⃣ Extração de Dados

#### Extração Completa
```bash
01-executar_extracao_completa.bat
```
**Função:** Extrai dados de todas as APIs (REST, GraphQL, Data Export)  
**Quando usar:** Extração diária ou completa  
**Tempo:** ~5-10 minutos  
**Resultado:** Dados salvos no SQL Server

#### Testar API Específica
```bash
02-testar_api_especifica.bat
```
**Função:** Testa uma API específica (REST, GraphQL ou Data Export)  
**Quando usar:** Testar uma API isoladamente  
**Tempo:** ~2-3 minutos por API

---

### 3️⃣ Validação

#### Validar Configurações
```bash
03-validar_config.bat
```
**Função:** Valida conexões e configurações  
**Quando usar:** Antes da primeira execução ou após mudanças  
**Resultado:** Relatório de validação

#### Executar Auditoria
```bash
04-executar_auditoria.bat
```
**Função:** Executa auditoria de integridade dos dados  
**Quando usar:** Após extração para validar qualidade  
**Resultado:** Relatório de auditoria

---

### 4️⃣ Exportação

#### Exportar para CSV/Excel
```bash
06-exportar_csv.bat
```
**Função:** Exporta todos os dados para arquivos CSV  
**Quando usar:** Após extração, para análise em Excel  
**Resultado:** Arquivos CSV na pasta `exports/`

**Arquivos gerados:**
- `faturas_a_pagar.csv` - Lançamentos a Pagar
- `faturas_a_receber.csv` - Lançamentos a Receber
- `fretes.csv` - Fretes
- `coletas.csv` - Coletas
- `manifestos.csv` - Manifestos
- `cotacoes.csv` - Cotações
- `localizacao_carga.csv` - Localização da Carga
- `ocorrencias.csv` - Ocorrências

---

## 🔄 Fluxo Recomendado

### Primeira Execução
```bash
# 1. Compilar
05-compilar_projeto.bat

# 2. Validar configurações
03-validar_config.bat

# 3. Executar extração
01-executar_extracao_completa.bat

# 4. Executar auditoria
04-executar_auditoria.bat

# 5. Exportar para CSV
06-exportar_csv.bat
```

### Execução Diária
```bash
# 1. Executar extração
01-executar_extracao_completa.bat

# 2. Exportar para CSV (opcional)
06-exportar_csv.bat
```

### Após Mudanças no Código
```bash
# 1. Compilar
05-compilar_projeto.bat

# 2. Testar API específica
02-testar_api_especifica.bat

# 3. Se OK, executar extração completa
01-executar_extracao_completa.bat
```

---

## 📊 Estrutura de Pastas

```
script-automacao/
├── 01-executar_extracao_completa.bat
├── 02-testar_api_especifica.bat
├── 03-validar_config.bat
├── 04-executar_auditoria.bat
├── 05-compilar_projeto.bat
├── 06-exportar_csv.bat
├── mvn.bat                    (wrapper Maven)
├── exports/                   (arquivos CSV gerados)
├── logs/                      (logs de execução)
├── relatorios/                (relatórios de auditoria)
└── target/                    (JAR compilado)
```

---

## 🛠️ Utilitários

### Wrapper Maven
```bash
mvn.bat
```
**Função:** Configura JAVA_HOME automaticamente  
**Uso:** Transparente - use `mvn` normalmente

---

## ⚠️ Troubleshooting

### Erro: JAR não encontrado
**Solução:** Execute `05-compilar_projeto.bat`

### Erro: JAVA_HOME não configurado
**Solução:** Use `mvn clean package` (wrapper automático)

### Erro: Banco de dados não acessível
**Solução:** Execute `03-validar_config.bat`

### Erro: Nenhum CSV gerado
**Solução:** 
1. Execute `01-executar_extracao_completa.bat` primeiro
2. Verifique se há dados no banco

---

## 📚 Documentação Adicional

- `README.md` - Documentação principal
- `docs/README_ATUALIZACAO_REST.md` - Atualização v2.0
- `docs/GUIA_RAPIDO_v2.0.md` - Guia rápido
- `docs/EXEMPLOS_USO_NOVOS_CAMPOS.md` - Consultas SQL

---

## 🎯 Resumo Rápido

| Script | Função | Frequência |
|--------|--------|------------|
| `01-executar_extracao_completa.bat` | Extração completa | Diária |
| `02-testar_api_especifica.bat` | Testar API | Quando necessário |
| `03-validar_config.bat` | Validar config | Primeira vez |
| `04-executar_auditoria.bat` | Auditoria | Após extração |
| `05-compilar_projeto.bat` | Compilar | Após mudanças |
| `06-exportar_csv.bat` | Exportar CSV | Quando necessário |

---

**Versão:** 2.1.0  
**Data:** 04/11/2025

