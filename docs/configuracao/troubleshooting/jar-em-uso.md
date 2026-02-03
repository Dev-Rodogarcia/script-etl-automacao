# 🔧 Solução: JAR em Uso por Outro Processo

## ❌ Problema

```
Failed to delete target\extrator.jar: 
O arquivo já está sendo usado por outro processo
```

## 🎯 Causas Comuns

1. **Processo Java ainda rodando** - Extração anterior não finalizou
2. **Antivírus escaneando o arquivo** - Windows Defender ou outro
3. **Explorador de arquivos aberto** - Pasta target aberta
4. **IDE com o arquivo aberto** - IntelliJ, Eclipse, VS Code

---

## ✅ Soluções

### 🥇 Solução 1: Usar Script Rápido (Recomendado)

Se o projeto já está compilado, use o script que não recompila:

```bash
07-exportar_csv_rapido.bat
```

✅ Não precisa recompilar  
✅ Não tenta deletar o JAR  
✅ Mais rápido  

---

### 🥈 Solução 2: Fechar Processos Java

**Opção A: Fechar manualmente**
- Pressione `Ctrl+Shift+Esc` (Gerenciador de Tarefas)
- Procure por "java.exe"
- Clique com botão direito → "Finalizar tarefa"

**Opção B: Usar comando**
```bash
taskkill /F /IM java.exe
```

Depois execute:
```bash
07-exportar_csv.bat
```

---

### 🥉 Solução 3: Aguardar e Tentar Novamente

Às vezes o antivírus está escaneando o arquivo:

```bash
# Aguarde 10 segundos
timeout /t 10

# Tente novamente
07-exportar_csv.bat
```

---

### 🔧 Solução 4: Compilar Sem Clean

Se você só quer recompilar sem deletar:

```bash
mvn compile
```

Depois execute o exportador:
```bash
07-exportar_csv_rapido.bat
```

---

## 🚀 Fluxo Recomendado

### Para Primeira Execução:
```bash
# 1. Compilar tudo
mvn clean package

# 2. Exportar (sem recompilar)
07-exportar_csv_rapido.bat
```

### Para Execuções Seguintes:
```bash
# Apenas exportar (usa JAR existente)
07-exportar_csv_rapido.bat
```

### Se Mudou o Código:
```bash
# 1. Fechar processos Java
taskkill /F /IM java.exe

# 2. Recompilar
mvn clean package

# 3. Exportar
07-exportar_csv_rapido.bat
```

---

## 📝 Scripts Disponíveis

| Script | Função | Quando Usar |
|--------|--------|-------------|
| `07-exportar_csv_rapido.bat` | Exporta sem recompilar | **Recomendado** |
| `07-exportar_csv.bat` | Compila e exporta | Após mudanças no código |
| `06-exportar_dados_csv.bat` | Exportador alternativo | Se outros falharem |

---

## 💡 Dicas

### Evitar o Problema:

1. **Sempre feche as extrações anteriores** antes de recompilar
2. **Use o script rápido** quando não houver mudanças no código
3. **Adicione exceção no antivírus** para a pasta `target/`
4. **Feche a pasta target** no explorador de arquivos

### Verificar Processos Java:

```bash
# Listar processos Java
tasklist | findstr java

# Matar todos os processos Java
taskkill /F /IM java.exe
```

---

## 🐛 Troubleshooting

### Problema: Script rápido não funciona
**Causa:** JAR não existe  
**Solução:** Compile primeiro com `mvn clean package`

### Problema: Erro persiste após fechar Java
**Causa:** Antivírus ou explorador de arquivos  
**Solução:** 
1. Feche o explorador de arquivos
2. Aguarde 10 segundos
3. Tente novamente

### Problema: Não consigo matar o processo Java
**Causa:** Processo travado  
**Solução:** Reinicie o computador (última opção)

---

## ✅ Checklist

Antes de executar `07-exportar_csv.bat`:

- [ ] Nenhum processo Java rodando?
- [ ] Pasta target fechada no explorador?
- [ ] Antivírus não está escaneando?
- [ ] IDE não está com o arquivo aberto?

Se todos marcados, pode executar sem problemas!

---

## 🎯 Resumo

**Problema:** JAR em uso  
**Solução Rápida:** Use `07-exportar_csv_rapido.bat`  
**Solução Completa:** Feche Java + `07-exportar_csv.bat`

---

**Recomendação:** Use sempre o script rápido para exportações rotineiras!

