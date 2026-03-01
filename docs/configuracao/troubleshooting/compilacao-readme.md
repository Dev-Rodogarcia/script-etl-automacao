# ⚡ Guia de Compilação - Atualização v2.0

## 🎯 Três Formas de Compilar

### 1️⃣ Forma Mais Simples (Recomendada)
```bash
# Configura e compila automaticamente
00-configurar_java_e_compilar.bat
```
✅ Tudo em um comando  
✅ Não precisa configurar nada  

---

### 2️⃣ Usando o Script Original
```bash
# Agora funciona automaticamente!
05-compilar_projeto.bat
```
✅ Script atualizado com JAVA_HOME  
✅ Funciona como antes  

---

### 3️⃣ Usando Maven Diretamente

**Passo 1:** Configure o ambiente (uma vez por sessão)
```bash
set-java-env.bat
```

**Passo 2:** Use Maven normalmente
```bash
mvn clean package
mvn clean install
mvn test
# etc...
```
✅ Controle total  
✅ Comandos Maven padrão  

---

## 🚀 Após Compilar

Quando ver:
```
BUILD SUCCESS
JAR gerado: target\extrator.jar
```

Execute:
```bash
01-executar_extracao_completa.bat
```

---

## 📝 O Que Foi Corrigido

### Antes (v1.0)
```bash
mvn clean package
# ❌ Erro: JAVA_HOME not defined
```

### Agora (v2.0)
```bash
# Opção 1
00-configurar_java_e_compilar.bat
# ✅ Funciona!

# Opção 2
05-compilar_projeto.bat
# ✅ Funciona!

# Opção 3
set-java-env.bat
mvn clean package
# ✅ Funciona!
```

---

## 🔧 Scripts Disponíveis

| Script | Função | Quando Usar |
|--------|--------|-------------|
| `00-configurar_java_e_compilar.bat` | Configura + Compila | Primeira vez ou compilação rápida |
| `05-compilar_projeto.bat` | Compila (com JAVA_HOME) | Compilação padrão |
| `set-java-env.bat` | Apenas configura ambiente | Usar Maven manualmente |
| `00-definir_java_home.bat` | Configura permanente | Requer admin (não recomendado) |

---

## 💡 Dicas

### Para desenvolvimento diário:
```bash
# Configure uma vez
set-java-env.bat

# Compile quantas vezes quiser
mvn clean package
mvn clean package
mvn clean package
```

### Para compilação rápida:
```bash
# Tudo em um comando
00-configurar_java_e_compilar.bat
```

### Para usar o script original:
```bash
# Agora funciona automaticamente
05-compilar_projeto.bat
```

---

## 🐛 Problemas?

Veja: `COMO_COMPILAR.md` para troubleshooting completo

---

## ✅ Checklist

- [x] Scripts criados
- [x] JAVA_HOME configurado automaticamente
- [x] `05-compilar_projeto.bat` atualizado
- [x] Três formas de compilar disponíveis
- [x] Documentação completa

---

**Recomendação:** Use `00-configurar_java_e_compilar.bat` ou `05-compilar_projeto.bat`

**Versão:** 2.0.0  
**Status:** ✅ Pronto para Uso

