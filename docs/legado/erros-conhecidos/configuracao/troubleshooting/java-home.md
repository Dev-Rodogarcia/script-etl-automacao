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
# 🔧 Solução: JAVA_HOME não configurado

## ❌ Problema

```
The JAVA_HOME environment variable is not defined correctly,
this environment variable is needed to run this program.
```

## ✅ Soluções

### Opção 1: Configuração Temporária (Recomendado)

Execute o script que configura e compila automaticamente:

```bash
00-configurar_java_e_compilar.bat
```

**Vantagens:**
- ✅ Não requer privilégios de administrador
- ✅ Compila automaticamente
- ✅ Funciona imediatamente

**Desvantagens:**
- ⚠️ Precisa executar este script toda vez

---

### Opção 2: Configuração Permanente

Execute como **Administrador**:

```bash
# Clique com botão direito → "Executar como administrador"
00-definir_java_home.bat
```

Depois, **feche e reabra** o prompt de comando e execute:

```bash
05-compilar_projeto.bat
```

**Vantagens:**
- ✅ Configuração permanente
- ✅ Funciona em todos os prompts

**Desvantagens:**
- ⚠️ Requer privilégios de administrador
- ⚠️ Precisa reiniciar o prompt

---

### Opção 3: Configuração Manual

1. **Abra as Variáveis de Ambiente:**
   - Pressione `Win + Pause/Break`
   - Clique em "Configurações avançadas do sistema"
   - Clique em "Variáveis de Ambiente"

2. **Adicione JAVA_HOME:**
   - Em "Variáveis do sistema", clique em "Novo"
   - Nome: `JAVA_HOME`
   - Valor: `C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot`
   - Clique em "OK"

3. **Atualize o PATH:**
   - Selecione "Path" em "Variáveis do sistema"
   - Clique em "Editar"
   - Adicione: `%JAVA_HOME%\bin`
   - Clique em "OK"

4. **Reinicie o prompt** e execute:
   ```bash
   05-compilar_projeto.bat
   ```

---

## 🧪 Verificar Configuração

Após configurar, teste:

```bash
# Verificar JAVA_HOME
echo %JAVA_HOME%
# Deve mostrar: C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot

# Verificar Java
java -version
# Deve mostrar: openjdk version "17.0.16"

# Verificar Maven
mvn -version
# Deve mostrar a versão do Maven e usar o Java correto
```

---

## 🚀 Próximos Passos

Após compilar com sucesso:

1. ✅ Execute a extração:
   ```bash
   01-executar_extracao_completa.bat
   ```

2. ✅ Valide os dados:
   ```sql
   SELECT TOP 10 * FROM faturas_a_pagar
   ORDER BY data_extracao DESC;
   ```

3. ✅ Consulte a documentação:
   ```
   docs/README_ATUALIZACAO_REST.md
   docs/GUIA_RAPIDO_v2.0.md
   ```

---

## 📞 Suporte

Se o problema persistir:

1. Verifique se o Java está instalado:
   ```bash
   where java
   ```

2. Verifique se o Maven está instalado:
   ```bash
   where mvn
   ```

3. Consulte os logs de compilação para erros específicos

---

**Recomendação:** Use a **Opção 1** (00-configurar_java_e_compilar.bat) para começar rapidamente!

