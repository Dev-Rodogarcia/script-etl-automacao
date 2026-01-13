# 📚 Documentação - ESL Cloud Extrator

## 🎯 Navegação Rápida

### 📘 Início Rápido
- [**Leia-me Primeiro**](01-inicio-rapido/leia-me-primeiro.md) - Comece aqui
- [**Guia Rápido**](01-inicio-rapido/guia-rapido.md) - 5 minutos para começar
- [**Scripts**](01-inicio-rapido/scripts.md) - Todos os scripts disponíveis
- [**Início Rápido**](01-inicio-rapido/inicio-rapido.md) - 3 passos para começar

### 🔌 Documentação de APIs
- [**API REST**](02-apis/rest/) - Faturas a Pagar, Faturas a Receber, Ocorrências
- [**API GraphQL**](02-apis/graphql/) - Coletas, Fretes
- [**API DataExport**](02-apis/dataexport/) - Manifestos, Cotações, Localização de Carga
- [**Análise Crítica**](02-apis/analise-critica.md) - Análise completa dos endpoints

### ⚙️ Configuração e Troubleshooting
- [**Insomnia**](03-configuracao/insomnia/) - Guias de instalação e uso do Insomnia
- [**Troubleshooting**](03-configuracao/troubleshooting/) - Solução de problemas comuns

### 📋 Especificações Técnicas
- [**Implementação de APIs**](04-especificacoes-tecnicas/implementacao-apis/) - Especificações técnicas completas

### 📦 Versões
- [**Versão 2.0**](05-versoes/v2.0/) - Documentação da versão 2.0/2.1

### 📖 Referências
- [**CSVs**](06-referencias/csvs/) - Referências de CSVs e como converter XLSX
- [**Mapeamento**](06-referencias/mapeamento/) - Templates de mapeamento

### 💡 Ideias Futuras
- [**Recomendações**](07-ideias-futuras/) - Ideias e melhorias futuras

### 📄 Documentos Gerais
- [**Resumo Executivo**](00-documentos-gerais/resumo-executivo.md) - Resumo executivo do projeto
- [**Entrega Completa**](00-documentos-gerais/entrega-completa.md) - Documentação de entrega completa
- [**Solicitação de Suporte**](00-documentos-gerais/solicitacao-suporte-esl-autenticacao-api.md) - Solicitação de suporte ESL

---

## 📁 Estrutura da Documentação

```
docs/
├── 00-documentos-gerais/           # Documentos gerais do projeto
│   ├── resumo-executivo.md
│   ├── entrega-completa.md
│   └── solicitacao-suporte-esl-autenticacao-api.md
│
├── 01-inicio-rapido/              # Guias de início rápido
│   ├── leia-me-primeiro.md
│   ├── guia-rapido.md
│   ├── scripts.md
│   ├── inicio-rapido.md
│   └── banners-estilizados.md
│
├── 02-apis/                       # Documentação de APIs
│   ├── rest/                      # API REST
│   │   ├── faturas-a-pagar.md
│   │   ├── faturas-a-receber.md
│   │   └── ocorrencias.md
│   ├── graphql/                   # API GraphQL
│   │   ├── coletas.md
│   │   └── fretes.md
│   ├── dataexport/                # API DataExport
│   │   ├── manifestos.md
│   │   ├── cotacoes.md
│   │   └── localizacao-carga.md
│   └── analise-critica.md
│
├── 03-configuracao/               # Configuração e troubleshooting
│   ├── insomnia/                  # Documentação do Insomnia
│   │   ├── instalacao.md
│   │   ├── requisicoes-rest.md
│   │   ├── requisicoes-graphql.md
│   │   ├── requisicoes-dataexport.md
│   │   ├── obter-tokens.md
│   │   ├── guia-rapido.md
│   │   └── analise-resposta-manifestos.md
│   └── troubleshooting/           # Solução de problemas
│       ├── compilacao.md
│       ├── maven.md
│       ├── java-home.md
│       └── jar-em-uso.md
│
├── 04-especificacoes-tecnicas/    # Especificações técnicas
│   └── implementacao-apis/
│       ├── design.md
│       ├── requirements.md
│       ├── technical-specification.md
│       └── ...
│
├── 05-versoes/                    # Documentação de versões
│   └── v2.0/
│       ├── release-notes.md
│       ├── exemplos-uso.md
│       ├── checklist-validacao.md
│       └── ...
│
├── 06-referencias/                # Referências e templates
│   ├── csvs/                      # Referências de CSVs
│   │   ├── como-converter-xlsx.md
│   │   └── evidencias-para-buscar.md
│   └── mapeamento/                # Templates de mapeamento
│       └── template-mapeamento.md
│
├── 07-ideias-futuras/             # Ideias e recomendações
│   ├── recomendacoes-melhorias.md
│   └── ...
│
├── 08-arquivos-secretos/          # ⚠️ Arquivos sensíveis (não versionados)
│   ├── armazenamento.md
│   ├── dataexport-guia.md
│   └── ...
│
├── relatorios-diarios/            # 📊 Relatórios diários (mantido como está)
│
└── README.md                      # Este arquivo (único arquivo na raiz)
```

---

## 🚀 Início Rápido

### 1. Primeira Vez?
Leia: [**Leia-me Primeiro**](01-inicio-rapido/leia-me-primeiro.md)

### 2. Quer Começar Rápido?
Leia: [**Guia Rápido**](01-inicio-rapido/guia-rapido.md)

### 3. Problemas?
Veja: [**Troubleshooting**](03-configuracao/troubleshooting/)

### 4. Quer Entender as APIs?
Veja: [**Documentação de APIs**](02-apis/)

---

## 📊 Índice Completo da Documentação

### 01-inicio-rapido/
Guias para começar a usar o sistema rapidamente.

- [**leia-me-primeiro.md**](01-inicio-rapido/leia-me-primeiro.md) - Primeiros passos
- [**guia-rapido.md**](01-inicio-rapido/guia-rapido.md) - Guia rápido de 5 minutos
- [**scripts.md**](01-inicio-rapido/scripts.md) - Todos os scripts disponíveis
- [**inicio-rapido.md**](01-inicio-rapido/inicio-rapido.md) - 3 passos para começar
- [**banners-estilizados.md**](01-inicio-rapido/banners-estilizados.md) - Banners ASCII art

### 02-apis/
Documentação completa de todas as APIs disponíveis.

#### REST
- [**faturas-a-pagar.md**](02-apis/rest/faturas-a-pagar.md) - API de Faturas a Pagar
- [**faturas-a-receber.md**](02-apis/rest/faturas-a-receber.md) - API de Faturas a Receber
- [**ocorrencias.md**](02-apis/rest/ocorrencias.md) - API de Ocorrências

#### GraphQL
- [**coletas.md**](02-apis/graphql/coletas.md) - API GraphQL de Coletas
- [**fretes.md**](02-apis/graphql/fretes.md) - API GraphQL de Fretes

#### DataExport
- [**manifestos.md**](02-apis/dataexport/manifestos.md) - API DataExport de Manifestos
- [**cotacoes.md**](02-apis/dataexport/cotacoes.md) - API DataExport de Cotações
- [**localizacao-carga.md**](02-apis/dataexport/localizacao-carga.md) - API DataExport de Localização de Carga

#### Análise
- [**analise-critica.md**](02-apis/analise-critica.md) - Análise crítica dos endpoints

### 03-configuracao/
Configuração do sistema e solução de problemas.

#### Insomnia
- [**instalacao.md**](03-configuracao/insomnia/instalacao.md) - Instalação do Insomnia
- [**requisicoes-rest.md**](03-configuracao/insomnia/requisicoes-rest.md) - Requisições API REST
- [**requisicoes-graphql.md**](03-configuracao/insomnia/requisicoes-graphql.md) - Requisições API GraphQL
- [**requisicoes-dataexport.md**](03-configuracao/insomnia/requisicoes-dataexport.md) - Requisições API DataExport
- [**obter-tokens.md**](03-configuracao/insomnia/obter-tokens.md) - Como obter tokens
- [**guia-rapido.md**](03-configuracao/insomnia/guia-rapido.md) - Guia rápido de testes
- [**analise-resposta-manifestos.md**](03-configuracao/insomnia/analise-resposta-manifestos.md) - Análise de resposta de manifestos

#### Troubleshooting
- [**compilacao.md**](03-configuracao/troubleshooting/compilacao.md) - Guia de compilação
- [**maven.md**](03-configuracao/troubleshooting/maven.md) - Solução para Maven
- [**java-home.md**](03-configuracao/troubleshooting/java-home.md) - Configurar JAVA_HOME
- [**jar-em-uso.md**](03-configuracao/troubleshooting/jar-em-uso.md) - Resolver JAR em uso

### 04-especificacoes-tecnicas/
Especificações técnicas do sistema.

#### Implementação de APIs
- [**design.md**](04-especificacoes-tecnicas/implementacao-apis/design.md) - Design do sistema
- [**requirements.md**](04-especificacoes-tecnicas/implementacao-apis/requirements.md) - Requisitos
- [**technical-specification.md**](04-especificacoes-tecnicas/implementacao-apis/technical-specification.md) - Especificação técnica completa
- [**resumo-tecnico-graphql-dataexport.md**](04-especificacoes-tecnicas/implementacao-apis/resumo-tecnico-graphql-dataexport.md) - Resumo técnico GraphQL e DataExport

### 05-versoes/
Documentação de versões do sistema.

#### v2.0
- [**release-notes.md**](05-versoes/v2.0/release-notes.md) - Release notes
- [**exemplos-uso.md**](05-versoes/v2.0/exemplos-uso.md) - Exemplos de uso
- [**checklist-validacao.md**](05-versoes/v2.0/checklist-validacao.md) - Checklist de validação
- [**diagrama-estrutura.md**](05-versoes/v2.0/diagrama-estrutura.md) - Diagrama de estrutura
- [**sumario-executivo.md**](05-versoes/v2.0/sumario-executivo.md) - Sumário executivo
- [**atualizacao-rest.md**](05-versoes/v2.0/atualizacao-rest.md) - Atualização REST completa

### 06-referencias/
Referências e templates úteis.

#### CSVs
- [**como-converter-xlsx.md**](06-referencias/csvs/como-converter-xlsx.md) - Como converter XLSX para CSV
- [**evidencias-para-buscar.md**](06-referencias/csvs/evidencias-para-buscar.md) - Evidências para buscar
- Arquivos CSV e XLSX de referência

#### Mapeamento
- [**template-mapeamento.md**](06-referencias/mapeamento/template-mapeamento.md) - Template de mapeamento

### 07-ideias-futuras/
Ideias e recomendações para melhorias futuras.

- [**recomendacoes-melhorias.md**](07-ideias-futuras/recomendacoes-melhorias.md) - Recomendações de melhorias

### 08-arquivos-secretos/
⚠️ **Arquivos sensíveis que não são versionados no GitHub**

- [**armazenamento.md**](08-arquivos-secretos/armazenamento.md) - Informações de armazenamento
- [**dataexport-guia.md**](08-arquivos-secretos/dataexport-guia.md) - Guia do DataExport
- [**Rodogarcia.postman_collection.md**](08-arquivos-secretos/Rodogarcia.postman_collection.md) - Coleção Postman

### relatorios-diarios/
📊 **Relatórios diários de execução (mantido como está - não mexer)**

- Relatórios diários de execução do sistema
- Documentação de classes de APIs
- Dúvidas e pedidos de endpoints

---

## 📊 Documentação por Tipo

### 🔌 APIs
- **REST**: Faturas a Pagar, Faturas a Receber, Ocorrências
- **GraphQL**: Coletas, Fretes
- **DataExport**: Manifestos, Cotações, Localização de Carga

### ⚙️ Configuração
- **Insomnia**: Instalação, configuração e uso
- **Troubleshooting**: Solução de problemas comuns
- **Tokens**: Como obter e configurar tokens

### 📋 Especificações
- **Design**: Arquitetura e design do sistema
- **Requirements**: Requisitos funcionais e não funcionais
- **Technical Specification**: Especificação técnica completa

### 📦 Versões
- **v2.0/2.1**: Documentação da versão atual

---

## 🚀 Fluxo de Leitura Recomendado

### Para Começar
1. [README.md](README.md) - Visão geral da documentação
2. [01-inicio-rapido/leia-me-primeiro.md](01-inicio-rapido/leia-me-primeiro.md) - Primeiros passos
3. [01-inicio-rapido/scripts.md](01-inicio-rapido/scripts.md) - Conhecer os scripts

### Para Desenvolver
1. [02-apis/](02-apis/) - Entender as APIs disponíveis
2. [04-especificacoes-tecnicas/implementacao-apis/design.md](04-especificacoes-tecnicas/implementacao-apis/design.md) - Arquitetura
3. [05-versoes/v2.0/exemplos-uso.md](05-versoes/v2.0/exemplos-uso.md) - Exemplos práticos

### Para Configurar
1. [03-configuracao/insomnia/instalacao.md](03-configuracao/insomnia/instalacao.md) - Instalar Insomnia
2. [03-configuracao/insomnia/obter-tokens.md](03-configuracao/insomnia/obter-tokens.md) - Obter tokens
3. [03-configuracao/insomnia/requisicoes-rest.md](03-configuracao/insomnia/requisicoes-rest.md) - Testar APIs

### Para Resolver Problemas
1. [03-configuracao/troubleshooting/compilacao.md](03-configuracao/troubleshooting/compilacao.md) - Compilação
2. [03-configuracao/troubleshooting/maven.md](03-configuracao/troubleshooting/maven.md) - Problemas com Maven
3. [03-configuracao/troubleshooting/java-home.md](03-configuracao/troubleshooting/java-home.md) - JAVA_HOME

### Para Apresentar
1. [00-documentos-gerais/resumo-executivo.md](00-documentos-gerais/resumo-executivo.md) - Resumo executivo
2. [05-versoes/v2.0/sumario-executivo.md](05-versoes/v2.0/sumario-executivo.md) - Sumário executivo
3. [05-versoes/v2.0/release-notes.md](05-versoes/v2.0/release-notes.md) - Release notes

---

## ✨ Novidades v2.1

- ✅ **15 campos disponíveis** (era 11)
- ✅ **Status automático** (Pendente/Vencido)
- ✅ **Forma de pagamento** traduzida
- ✅ **Competência** calculada (YYYY-MM)
- ✅ **Banners estilizados** ASCII art
- ✅ **Scripts organizados** (01-06)
- ✅ **Documentação reorganizada** em estrutura hierárquica clara

---

## 📊 Estatísticas da Documentação

- **Total de APIs documentadas:** 8
  - REST: 3
  - GraphQL: 2
  - DataExport: 3

- **Total de guias:** 15+
- **Total de especificações técnicas:** 8
- **Total de versões documentadas:** 1 (v2.0/2.1)

---

## 🔒 Arquivos Sensíveis

⚠️ **IMPORTANTE**: A pasta `08-arquivos-secretos/` contém arquivos sensíveis e **não é versionada no GitHub**. Esta pasta está configurada no `.gitignore`.

---

## 📞 Suporte

- **Documentação:** Você está aqui!
- **README Principal:** [../README.md](../README.md)

---

**Versão:** 2.1.0  
**Data:** 07/11/2025  
**Última Atualização:** Reorganização completa da documentação - apenas README.md na raiz
