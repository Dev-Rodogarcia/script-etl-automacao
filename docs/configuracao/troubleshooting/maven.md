# 🎯 Solução Definitiva - Usar Maven Normalmente

## ❌ Problema

Você quer usar `mvn clean package` diretamente no prompt, mas recebe:
```
The JAVA_HOME environment variable is not defined correctly
```

## ✅ Solução Definitiva (Escolha uma)

### 🥇 Opção 1: Usar o Wrapper Maven (Mais Simples)

Agora você tem um arquivo `mvn.bat` no diretório do projeto que configura tudo automaticamente.

**Use Maven normalmente:**
```bash
mvn clean package
mvn clean compile
mvn clean install
mvn test
```

✅ Funciona exatamente como o Maven normal  
✅ Configura JAVA_HOME automaticamente  
✅ Não precisa fazer nada extra  

**Como funciona:**
- O arquivo `mvn.bat` no diretório do projeto é executado primeiro
- Ele configura o JAVA_HOME
- Depois chama o Maven real

---

### 🥈 Opção 2: Inicializar o Ambiente

Execute uma vez e mantenha o prompt aberto:

```bash
init-env.bat
```

Depois use Maven normalmente:
```bash
mvn clean package
mvn clean compile
mvn clean install
```

✅ Configura o ambiente completo  
✅ Abre um novo prompt configurado  
✅ Funciona enquanto o prompt estiver aberto  

---

### 🥉 Opção 3: Configurar Manualmente (Cada Sessão)

Execute antes de usar Maven:
```bash
set-java-env.bat
```

Depois use Maven:
```bash
mvn clean package
```

---

## 🎯 Recomendação

**Use a Opção 1 (Wrapper Maven):**

Simplesmente execute:
```bash
mvn clean package
```

O arquivo `mvn.bat` no diretório do projeto cuida de tudo automaticamente!

---

## 🧪 Testar

```bash
# Teste 1: Verificar Maven
mvn -version

# Teste 2: Compilar
mvn clean compile

# Teste 3: Gerar JAR
mvn clean package

# Teste 4: Sem testes
mvn clean package -DskipTests
```

---

## 📝 Como Funciona o Wrapper

Quando você digita `mvn` no prompt:

1. Windows procura `mvn.bat` no diretório atual **primeiro**
2. Encontra o `mvn.bat` do projeto
3. Este script configura JAVA_HOME
4. Chama o Maven real com seus argumentos
5. Tudo funciona normalmente!

---

## 🔧 Estrutura de Arquivos

```
script-automacao/
├── mvn.bat                          ← Wrapper Maven (NOVO)
├── init-env.bat                     ← Inicializa ambiente (NOVO)
├── set-java-env.bat                 ← Configura ambiente
├── 00-configurar_java_e_compilar.bat
├── 05-compilar_projeto.bat
└── pom.xml
```

---

## 💡 Dicas

### Para desenvolvimento diário:
```bash
# Opção A: Use mvn diretamente (wrapper automático)
mvn clean package

# Opção B: Inicialize o ambiente uma vez
init-env.bat
# Depois use mvn normalmente
```

### Para compilação rápida:
```bash
# Use o script completo
00-configurar_java_e_compilar.bat
```

---

## ⚠️ Importante

O arquivo `mvn.bat` deve estar no **mesmo diretório** onde você executa os comandos Maven (raiz do projeto).

Se você mudar de diretório, o wrapper não funcionará. Nesse caso, use `init-env.bat` ou `set-java-env.bat`.

---

## 🎉 Resultado

Agora você pode usar Maven exatamente como esperado:

```bash
C:\...\script-automacao> mvn clean package
[INFO] Scanning for projects...
[INFO] Building extrator 1.0-SNAPSHOT
[INFO] --------------------------------
...
[INFO] BUILD SUCCESS
```

✅ Sem erros de JAVA_HOME  
✅ Funciona como Maven normal  
✅ Nada de scripts extras  

---

**Solução:** Use `mvn clean package` diretamente - o wrapper cuida do resto!

