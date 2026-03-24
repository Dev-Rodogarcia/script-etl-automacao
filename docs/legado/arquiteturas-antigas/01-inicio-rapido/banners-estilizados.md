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
# 🎨 Banners Estilizados - Guia de Uso

## 📋 Visão Geral

O sistema agora exibe banners estilizados em ASCII art durante as extrações, tornando a experiência mais visual e profissional.

---

## 🎯 Banners Disponíveis

### 1. Banner de Extração Completa
**Arquivo:** `banner-extracao-completa.txt`  
**Quando:** Ao executar `01-executar_extracao_completa.bat`  
**Exibe:** Todas as APIs (REST, GraphQL, Data Export)

### 2. Banner API REST
**Arquivo:** `banner-api-rest.txt`  
**Quando:** Ao extrair dados da API REST  
**Exibe:** Faturas a Pagar (v2.1) e Ocorrências

### 3. Banner API GraphQL
**Arquivo:** `banner-api-graphql.txt`  
**Quando:** Ao extrair dados da API GraphQL  
**Exibe:** Coletas e Fretes

### 4. Banner API Data Export
**Arquivo:** `banner-api-dataexport.txt`  
**Quando:** Ao extrair dados da API Data Export  
**Exibe:** Manifestos, Cotações e Localização

### 5. Banner de Sucesso
**Arquivo:** `banner-sucesso.txt`  
**Quando:** Ao concluir extração com sucesso  
**Exibe:** Resumo e estatísticas

### 6. Banner de Erro
**Arquivo:** `banner-erro.txt`  
**Quando:** Ao ocorrer erro na extração  
**Exibe:** Detalhes do erro

---

## 💻 Como Usar no Código

### Importar a Classe
```java
import br.com.extrator.util.BannerUtil;
```

### Exibir Banners
```java
// Banner de extração completa
BannerUtil.exibirBannerExtracaoCompleta();

// Banner API REST
BannerUtil.exibirBannerApiRest();

// Banner API GraphQL
BannerUtil.exibirBannerApiGraphQL();

// Banner API Data Export
BannerUtil.exibirBannerApiDataExport();

// Banner de sucesso
BannerUtil.exibirBannerSucesso();

// Banner de erro
BannerUtil.exibirBannerErro();
```

### Exibir Estatísticas
```java
// Exibir estatísticas formatadas
BannerUtil.exibirEstatisticas("Faturas a Pagar", 1250, 45);
// Saída:
//   📊 Faturas a Pagar
//      ├─ Registros: 1250
//      ├─ Tempo: 45s
//      └─ Taxa: 27 reg/s
```

### Mensagens Formatadas
```java
// Progresso
BannerUtil.exibirProgresso("Conectando ao banco de dados");
// Saída: ⏳ Conectando ao banco de dados...

// Sucesso
BannerUtil.exibirSucessoMensagem("Dados salvos com sucesso");
// Saída: ✅ Dados salvos com sucesso

// Erro
BannerUtil.exibirErroMensagem("Falha na conexão");
// Saída: ❌ Falha na conexão

// Aviso
BannerUtil.exibirAvisoMensagem("Alguns registros foram ignorados");
// Saída: ⚠️  Alguns registros foram ignorados
```

---

## 📝 Exemplo Completo

### Extração REST
```java
public class RestRunner {
    
    public void executar() {
        // Exibir banner inicial
        BannerUtil.exibirBannerApiRest();
        
        // Progresso
        BannerUtil.exibirProgresso("Conectando à API REST");
        
        try {
            // Extração
            final long inicio = System.currentTimeMillis();
            final int registros = extrairDados();
            final long tempo = (System.currentTimeMillis() - inicio) / 1000;
            
            // Sucesso
            BannerUtil.exibirBannerSucesso();
            BannerUtil.exibirEstatisticas("Faturas a Pagar", registros, tempo);
            BannerUtil.exibirSucessoMensagem("Extração concluída!");
            
        } catch (Exception e) {
            // Erro
            BannerUtil.exibirBannerErro();
            BannerUtil.exibirErroMensagem("Falha na extração: " + e.getMessage());
        }
    }
}
```

---

## 🎨 Personalização

### Criar Novo Banner

1. **Criar arquivo de texto:**
   ```
   src/main/resources/banners/meu-banner.txt
   ```

2. **Adicionar método em BannerUtil:**
   ```java
   public static void exibirMeuBanner() {
       exibirBanner("banners/meu-banner.txt");
   }
   ```

3. **Usar no código:**
   ```java
   BannerUtil.exibirMeuBanner();
   ```

### Dicas de Design

- Use ferramentas online para gerar ASCII art
- Mantenha largura máxima de 80 caracteres
- Use caracteres Unicode para símbolos (✅ ❌ ⚠️ 📊 🚀)
- Teste em diferentes terminais

---

## 📊 Estrutura de Arquivos

```
src/main/resources/banners/
├── banner-extracao-completa.txt
├── banner-api-rest.txt
├── banner-api-graphql.txt
├── banner-api-dataexport.txt
├── banner-sucesso.txt
└── banner-erro.txt

src/main/java/br/com/extrator/util/
└── BannerUtil.java
```

---

## 🎯 Benefícios

### Visual
- ✅ Interface mais profissional
- ✅ Fácil identificação do tipo de extração
- ✅ Feedback visual claro

### Funcional
- ✅ Estatísticas formatadas
- ✅ Mensagens padronizadas
- ✅ Fácil manutenção

### Experiência
- ✅ Mais agradável de usar
- ✅ Informações organizadas
- ✅ Status claro do processo

---

## 📝 Exemplo de Saída

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                    EXTRATOR DE DADOS - VERSÃO 2.1                           ║
╚══════════════════════════════════════════════════════════════════════════════╝

┌──────────────────────────────────────────────────────────────────────────────┐
│                           🔵 API REST - EXTRAÇÃO                             │
└──────────────────────────────────────────────────────────────────────────────┘

  ⏳ Conectando à API REST...
  ⏳ Extraindo faturas a pagar...
  ✅ 1250 registros extraídos

╔═══════════════════════════════════════════════════════════════════════════╗
║                    ✅ EXTRAÇÃO CONCLUÍDA COM SUCESSO!                     ║
╚═══════════════════════════════════════════════════════════════════════════╝

  📊 Faturas a Pagar
     ├─ Registros: 1250
     ├─ Tempo: 45s
     └─ Taxa: 27 reg/s

  ✅ Extração concluída!
```

---

## 🔧 Integração com Scripts .bat

Os scripts .bat podem exibir os banners usando:

```batch
@echo off
type src\main\resources\banners\banner-extracao-completa.txt
echo.
echo Iniciando extracao...
```

---

## ✅ Checklist de Implementação

- [x] Banners criados (6 arquivos)
- [x] Classe BannerUtil implementada
- [x] Métodos auxiliares adicionados
- [x] Documentação completa
- [ ] Integrar nos Runners (próximo passo)
- [ ] Testar em diferentes terminais
- [ ] Adicionar aos scripts .bat

---

**Versão:** 2.1.0  
**Data:** 04/11/2025  
**Status:** ✅ Implementado

