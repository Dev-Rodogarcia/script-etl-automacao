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
# 🔧 Como Compilar o Projeto

## Problema: JAVA_HOME não configurado

Quando você executa `mvn clean package` e recebe:
```
The JAVA_HOME environment variable is not defined correctly
```

## ✅ Solução Rápida (Recomendada)

### Opção 1: Usar o script completo
```bash
# Este script configura e compila automaticamente
00-configurar_java_e_compilar.bat
```

### Opção 2: Configurar o ambiente e compilar manualmente

**Passo 1:** Configure o ambiente
```bash
set-java-env.bat
```

**Passo 2:** Compile normalmente
```bash
mvn clean package
```

**Passo 3:** Ou compile sem testes
```bash
mvn clean package -DskipTests
```

## 📝 Explicação

O problema ocorre porque o Maven não encontra o JAVA_HOME. Os scripts acima configuram:

```batch
JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot
PATH=%JAVA_HOME%\bin;%PATH%
```

## 🎯 Uso Diário

### Para compilar rapidamente:
```bash
# Opção A: Tudo em um comando
00-configurar_java_e_compilar.bat

# Opção B: Configurar uma vez e usar mvn várias vezes
set-java-env.bat
mvn clean package
mvn clean install
mvn test
# etc...
```

## ⚠️ Importante

- O `set-java-env.bat` configura apenas para a **sessão atual** do prompt
- Se você fechar e abrir um novo prompt, precisa executar novamente
- Para configuração permanente, use o script como administrador (não recomendado)

## 🚀 Após Compilar

Quando a compilação for bem-sucedida:

```
BUILD SUCCESS
JAR gerado: target\extrator.jar
```

Execute a extração:
```bash
01-executar_extracao_completa.bat
```

## 📚 Comandos Maven Úteis

```bash
# Compilar sem testes
mvn clean package -DskipTests

# Compilar com testes
mvn clean package

# Apenas compilar (sem gerar JAR)
mvn clean compile

# Limpar build anterior
mvn clean

# Ver versão do Maven
mvn -version
```

## 🐛 Troubleshooting

### Maven não encontrado
```bash
# Verifique se Maven está instalado
mvn -version

# Se não funcionar, instale Maven:
# https://maven.apache.org/download.cgi
```

### Java não encontrado
```bash
# Verifique se Java está instalado
java -version

# Deve mostrar: openjdk version "17.0.16"
```

### Caminho do Java diferente
Se o Java estiver em outro local, edite os scripts:
```batch
# Altere esta linha nos scripts:
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"

# Para o caminho correto do seu Java
```

---

**Recomendação:** Use `00-configurar_java_e_compilar.bat` para começar rapidamente!

