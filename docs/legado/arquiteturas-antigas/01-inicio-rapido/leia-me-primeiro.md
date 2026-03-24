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
# 🚀 LEIA-ME PRIMEIRO - Atualização v2.0

## ⚡ Início Rápido (3 Passos)

### 1️⃣ Compilar
```bash
# Agora funciona diretamente!
mvn clean package
```

### 2️⃣ Executar
```bash
01-executar_extracao_completa.bat
```

### 3️⃣ Validar
```sql
SELECT TOP 10 * FROM faturas_a_pagar 
ORDER BY data_extracao DESC;
```

---

## 🎯 O Que Mudou na v2.0?

### ✨ Novos Recursos
- ✅ **+27% mais dados** (14 vs 11 campos)
- ✅ **Status automático** (Pendente/Vencido)
- ✅ **Análise por filial** (CNPJ + nome)
- ✅ **Dados contábeis** (conta + centros de custo)
- ✅ **Maven funciona diretamente** (sem configuração manual)

### 🔧 Correções
- ✅ JAVA_HOME configurado automaticamente
- ✅ Scripts atualizados
- ✅ Wrapper Maven criado

---

## 📚 Documentação

### Início Rápido
- `SOLUCAO_DEFINITIVA.md` - Como usar Maven normalmente
- `INICIO_RAPIDO.md` - Começar em 3 passos
- `README_COMPILACAO.md` - Guia de compilação

### Atualização v2.0
- `docs/README_ATUALIZACAO_REST.md` - Guia completo
- `docs/GUIA_RAPIDO_v2.0.md` - Início em 5 minutos
- `docs/EXEMPLOS_USO_NOVOS_CAMPOS.md` - Consultas SQL
- `RELEASE_NOTES_v2.0.md` - Changelog completo

### Troubleshooting
- `COMO_COMPILAR.md` - Problemas de compilação
- `SOLUCAO_JAVA_HOME.md` - Problemas de JAVA_HOME

---

## 🛠️ Scripts Disponíveis

| Script | Função | Quando Usar |
|--------|--------|-------------|
| `mvn clean package` | Compila (wrapper automático) | **Recomendado** |
| `00-configurar_java_e_compilar.bat` | Configura + Compila | Alternativa |
| `05-compilar_projeto.bat` | Compila (script original) | Alternativa |
| `init-env.bat` | Inicializa ambiente | Desenvolvimento |
| `set-java-env.bat` | Configura ambiente | Manual |

---

## 🎓 Comandos Úteis

### Compilação
```bash
# Compilar e gerar JAR
mvn clean package

# Compilar sem testes (mais rápido)
mvn clean package -DskipTests

# Apenas compilar
mvn clean compile

# Limpar build
mvn clean
```

### Extração
```bash
# Extração completa
01-executar_extracao_completa.bat

# Testar API
02-testar_api_especifica.bat
```

### Validação
```sql
-- Dashboard rápido
SELECT 
    COUNT(*) as total,
    SUM(total_value) as valor_total,
    COUNT(CASE WHEN status = 'Vencido' THEN 1 END) as vencidas,
    COUNT(DISTINCT filial) as filiais
FROM faturas_a_pagar
WHERE data_extracao >= CAST(GETDATE() AS DATE);
```

---

## 🆘 Problemas?

### ❌ Maven não funciona
**Solução:** Veja `SOLUCAO_DEFINITIVA.md`

### ❌ JAVA_HOME não configurado
**Solução:** Execute `init-env.bat` ou use `mvn` diretamente (wrapper automático)

### ❌ Novos campos NULL
**Solução:** Normal para alguns campos. Verifique `header_metadata`

---

## 📊 Novos Campos Disponíveis

1. **corporation.cnpj** → cnpjFilial
2. **corporation.nickname** → filial
3. **receiver.cnpjCpf** → cnpjFornecedor
4. **comments** → observacoes
5. **accounting_planning_management.name** → contaContabil
6. **cost_centers[].name** → centroCusto
7. **[CALCULADO]** → status

---

## 🎉 Pronto para Usar!

```bash
# Compile
mvn clean package

# Execute
01-executar_extracao_completa.bat

# Valide
# (Abra SQL Server e consulte faturas_a_pagar)
```

---

**Versão:** 2.0.0  
**Data:** 04/11/2025  
**Status:** ✅ Pronto para Produção

**Desenvolvido com ❤️ por Kiro AI**

