# 🔄 Fluxograma Estruturado para Miro - Extração Últimas 24h

**Versão:** 2.3.1  
**Data:** 23/01/2026  
**Formato:** Texto estruturado para replicação no Miro  
**Fluxo:** Extração de dados das últimas 24h (dia atual)

---

## 📋 Como Usar Este Documento

Este documento contém o fluxograma completo do sistema em formato texto estruturado, ideal para replicar no Miro.

**Estrutura:**
- Cada caixa mostra: `[PASTA/CLASSE]` e `método()`
- Setas indicam fluxo de execução
- Caminhos paralelos são claramente separados
- Convergências são marcadas explicitamente
- Condições estão explícitas com `[SE...]` e `[SENÃO...]`

---

## 🎯 FLUXOGRAMA COMPLETO - EXTRAÇÃO ÚLTIMAS 24H

### ETAPA 1: INICIALIZAÇÃO
**O que faz:** Inicia o sistema, carrega configurações e identifica qual comando executar

**Legenda dos blocos (explicação para leigos):**
- **Bloco 1.1:** O programa Java começa a rodar. É o primeiro passo quando você executa o sistema. Quando você digita `java -jar extrator.jar` no terminal, este é o ponto de entrada que inicia tudo.

- **Bloco 1.2:** Prepara o sistema de registro de atividades (logs). Organiza arquivos antigos de log em pastas corretas para não bagunçar. Também configura um "gancho" que garante que os logs sejam salvos mesmo se o programa for fechado abruptamente.

- **Bloco 1.3:** Verifica se você digitou algum comando ao executar o programa. **O que são comandos?** Comandos são palavras especiais que você pode digitar após o nome do programa para dizer o que você quer fazer. Por exemplo: `java -jar extrator.jar --validar` (o comando é `--validar`). O sistema verifica se o array `args` (argumentos) está vazio (`args.length == 0`). **Por que verifica?** Porque se você não digitar nada, o sistema precisa usar um comando padrão (padrão de fábrica). Se você digitar algo, o sistema usa o que você pediu.

- **Bloco 1.4a:** Se você não digitou nada (ou seja, executou apenas `java -jar extrator.jar` sem nenhum comando), o sistema escolhe automaticamente o comando `"--fluxo-completo"`. **O que faz esse comando?** Ele executa a extração completa de todas as entidades de todas as APIs (Coletas, Fretes, Manifestos, Cotações, etc). **Por que é o padrão?** Porque é a operação mais comum - a maioria das vezes você quer extrair todos os dados das últimas 24 horas.

- **Bloco 1.4b:** Se você digitou um comando (por exemplo: `java -jar extrator.jar --validar`), o sistema pega o primeiro argumento (`args[0]`) e converte para letras minúsculas (`.toLowerCase()`). **Por que converte para minúsculas?** Para que `--VALIDAR`, `--Validar` e `--validar` funcionem da mesma forma (o sistema não diferencia maiúsculas de minúsculas). **Quais comandos você pode usar?** O sistema tem 14 comandos disponíveis:
  1. `--fluxo-completo` - Executa extração completa de todas as APIs (é o padrão se você não digitar nada)
  2. `--extracao-intervalo` - Extrai dados de um período específico (ex: de 01/01/2025 até 31/01/2025)
  3. `--loop` - Inicia um console interativo para executar extrações em loop (start/pause/resume/stop)
  4. `--validar` - Valida se as configurações estão corretas e se consegue conectar nas APIs
  5. `--ajuda` ou `--help` - Mostra uma lista de todos os comandos disponíveis e como usá-los
  6. `--introspeccao` - Faz uma análise do schema GraphQL da API (descobre quais campos existem)
  7. `--auditoria` - Executa uma auditoria dos dados extraídos (verifica se está tudo completo)
  8. `--testar-api` - Testa se consegue conectar em uma API específica (REST, GraphQL ou DataExport)
  9. `--limpar-tabelas` - Remove todos os dados das tabelas do banco de dados (cuidado!)
  10. `--verificar-timestamps` - Verifica os timestamps dos registros no banco
  11. `--verificar-timezone` - Verifica o fuso horário configurado
  12. `--validar-manifestos` - Valida especificamente os dados de manifestos
  13. `--validar-dados` - Valida todos os dados extraídos de forma completa
  14. `--exportar-csv` - Exporta os dados do banco para arquivos CSV

- **Bloco 1.5:** Agora que temos um comando definido (seja o padrão `--fluxo-completo` do bloco 1.4a, ou o comando que você digitou no bloco 1.4b), o sistema verifica se esse comando existe no dicionário de comandos. **Como verifica?** O sistema tem um mapa (HashMap) chamado `COMANDOS` que contém todos os 14 comandos listados acima. Ele verifica se o nome do comando está presente nesse mapa usando `COMANDOS.containsKey(nomeComando)`. **Por que verifica?** Para evitar que o sistema tente executar um comando que não existe (o que causaria erro). É como verificar se uma palavra está no dicionário antes de tentar procurar seu significado.

- **Bloco 1.6a:** Se o comando existe no dicionário (ou seja, `COMANDOS.containsKey(nomeComando)` retornou `true`), o sistema busca o objeto `Comando` correspondente no mapa usando `COMANDOS.get(nomeComando)`. **O que é esse objeto?** Cada comando é uma classe Java que implementa a interface `Comando` e tem um método `executar()`. Por exemplo, o comando `--fluxo-completo` é a classe `ExecutarFluxoCompletoComando`, o comando `--validar` é a classe `ValidarAcessoComando`, etc. **Por que busca o objeto?** Porque o sistema precisa do objeto para poder chamar o método `executar()` dele depois.

- **Bloco 1.6b:** Se o comando NÃO existe no dicionário (ou seja, `COMANDOS.containsKey(nomeComando)` retornou `false`), o sistema mostra uma mensagem de erro dizendo "Comando desconhecido: [nome do comando]" e exibe a mensagem de ajuda com todos os comandos disponíveis. **Como mostra a ajuda?** O sistema cria um objeto `ExibirAjudaComando` e chama o método `executar()` dele, que imprime no terminal uma lista formatada de todos os comandos e como usá-los. **Por que encerra?** Porque o sistema não sabe o que fazer com um comando que não existe. Ele não pode "adivinhar" o que você quis dizer, então mostra a ajuda e encerra o programa.

- **Bloco 1.7:** Se chegou aqui, significa que o comando existe (passou pelo bloco 1.6a) e foi encontrado no mapa. Agora o sistema chama o método `executar(args)` do objeto `Comando` que foi encontrado. **O que acontece aqui?** Depende de qual comando foi escolhido. Se foi `--fluxo-completo` (o padrão), ele inicia a extração completa de dados. Se foi `--validar`, ele valida as configurações. Se foi `--ajuda`, ele mostra a ajuda. Cada comando tem sua própria lógica de execução. **Por que funciona assim?** Porque o sistema usa o padrão de projeto "Command", onde cada ação é encapsulada em uma classe separada. Isso torna o código mais organizado, fácil de manter e permite adicionar novos comandos sem modificar o código existente.

```
┌─────────────────────────────────────────────────────────────┐
│ [1.1] [br.com.extrator/Main.java]                          │
│ main(String[] args)                                          │
│ Ponto de entrada do programa                                │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [1.2] [br.com.extrator/servicos/LoggingService.java]       │
│ iniciarCaptura("extracao_dados")                            │
│ Runtime.addShutdownHook()                                   │
│ organizarLogsTxtNaPastaLogs()                               │
│ Inicia sistema de logs e organiza arquivos                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [1.3] [DECISÃO] args.length == 0?                           │
│ Verifica se usuário passou comando                           │
└──────────┬───────────────────────────────┬──────────────────┘
           │ SIM                           │ NÃO
           │ (não passou)                  │ (passou)
           │                               │
           ▼                               ▼
┌──────────────────────┐      ┌──────────────────────────────┐
│ [1.4a] nomeComando = │      │ [1.4b] nomeComando = args[0] │
│ "--fluxo-completo"    │      │ .toLowerCase()                │
│ Usa comando padrão    │      │ Usa comando informado         │
└──────────┬───────────┘      └──────────┬───────────────────┘
           │                              │
           │                              │
           └──────────────┬───────────────┘
                          │
                          │ ═══════════════════════════════════
                          │ CONVERGÊNCIA: Ambos caminhos chegam
                          │ aqui porque, independente de ter sido
                          │ comando padrão (1.4a) ou informado (1.4b),
                          │ agora temos um nomeComando definido e
                          │ precisamos verificar se ele existe no
                          │ sistema antes de executar.
                          │ ═══════════════════════════════════
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ [1.5] [DECISÃO] COMANDOS.containsKey(nomeComando)?          │
│ Verifica se comando existe                                  │
│ (Ambos caminhos 1.4a e 1.4b convergem aqui)                 │
└──────────┬───────────────────────────────┬──────────────────┘
           │ SIM                           │ NÃO
           │ (comando existe)              │ (comando não existe)
           │                               │
           ▼                               ▼
┌──────────────────────┐      ┌──────────────────────────────┐
│ [1.6a] comando =     │      │ [1.6b] [comandos.console/     │
│ COMANDOS.get()       │      │  ExibirAjudaComando.java]     │
│ Busca comando        │      │ executar(args)                │
│                      │      │ Exibe ajuda e encerra         │
└──────────┬───────────┘      └──────────────────────────────┘
           │
           │
           ▼
┌─────────────────────────────────────────────────────────────┐
│ [1.7] [comandos.extracao/ExecutarFluxoCompletoComando.java] │
│ executar(args)                                               │
│ Inicia execução do fluxo completo                            │
└──────────────────────────┬──────────────────────────────────┘
```

---

### ETAPA 2: PREPARAÇÃO DO FLUXO COMPLETO
**O que faz:** Prepara a extração dos dados de hoje, cria o executor paralelo e submete as duas threads para execução simultânea

**Legenda dos blocos (explicação para leigos):**
- **Bloco 2.1:** O comando de fluxo completo foi recebido e aceito na Etapa 1 (bloco 1.7). Agora começa a preparar tudo para extrair os dados. Este é o método `executar()` da classe `ExecutarFluxoCompletoComando`. **O que faz?** Inicia o processo de extração completa de todas as entidades das APIs.

- **Bloco 2.2:** Define que vai buscar dados de HOJE (últimas 24 horas). **Como define?** Usa `LocalDate.now()` para pegar a data atual do sistema. Mostra uma mensagem bonita no terminal (banner) informando que começou a extração. Também registra o horário de início (`inicioExecucao = LocalDateTime.now()`) para calcular depois quanto tempo levou. **Por que dados de hoje?** Porque o sistema foi projetado para extrair dados das últimas 24 horas automaticamente quando executado sem parâmetros de data.

- **Bloco 2.3:** Cria um "gerenciador de tarefas paralelas" (ExecutorService) que vai permitir fazer 2 coisas ao mesmo tempo. **O que é um ExecutorService?** É uma ferramenta do Java que gerencia threads (trabalhadores). **Como cria?** Usa `Executors.newFixedThreadPool(2)` que cria um pool com exatamente 2 threads. Também cria estruturas de dados para controlar o que está acontecendo: um mapa (`runnersFuturos`) para guardar referências às threads que estão rodando, uma lista (`runnersFalhados`) para anotar quais falharam, e contadores (`totalSucessos` e `totalFalhas`) para saber quantos deram certo ou errado. **Por que 2 threads?** Porque temos 2 APIs principais para extrair: GraphQL e DataExport. Cada uma vai rodar em uma thread separada, trabalhando ao mesmo tempo.

- **Bloco 2.4:** Submete o Trabalhador 1 (Thread 1) para começar a trabalhar. **O que faz?** Chama `executor.submit()` passando uma tarefa que vai executar `GraphQLRunner.executar(dataHoje)`. Isso coloca a thread na fila do executor e ela começa a trabalhar imediatamente. A referência dessa thread (um objeto `Future`) é guardada no mapa `runnersFuturos` com a chave "GraphQL". **O que vai extrair?** Coletas e Fretes da API GraphQL. **Por que trabalha sozinho?** Porque é uma thread independente que não precisa esperar a Thread 2 começar ou terminar.

- **Bloco 2.5:** Submete o Trabalhador 2 (Thread 2) para começar a trabalhar. **O que faz?** Chama `executor.submit()` passando uma tarefa que vai executar `DataExportRunner.executar(dataHoje)`. Isso também coloca a thread na fila do executor e ela começa a trabalhar imediatamente, ao mesmo tempo que a Thread 1. A referência dessa thread também é guardada no mapa `runnersFuturos` com a chave "DataExport". **O que vai extrair?** Manifestos, Cotações, Localização de Carga, Contas a Pagar e Faturas por Cliente da API Data Export. **Por que trabalha ao mesmo tempo?** Porque as duas APIs são independentes - uma não depende da outra. Então faz sentido extrair ambas simultaneamente para economizar tempo.

```
┌─────────────────────────────────────────────────────────────┐
│ [2.1] [comandos.extracao/ExecutarFluxoCompletoComando.java] │
│ executar(args)                                               │
│ Recebe comando e inicia preparação                           │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [2.2] [INICIALIZAÇÃO]                                       │
│ BannerUtil.exibirBannerExtracaoCompleta()                  │
│ dataHoje = LocalDate.now()  ← DADOS DE HOJE (últimas 24h) │
│ inicioExecucao = LocalDateTime.now()                       │
│ Define data de hoje e exibe banner                          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [2.3] [CRIAÇÃO DO EXECUTOR PARALELO]                        │
│ ExecutorService executor =                                   │
│   Executors.newFixedThreadPool(2)                            │
│ Map<String, Future<?>> runnersFuturos = LinkedHashMap        │
│ List<String> runnersFalhados = ArrayList                     │
│ int totalSucessos = 0                                        │
│ int totalFalhas = 0                                         │
│ Cria executor para rodar 2 threads simultaneamente          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           │
        ┌──────────────────┴──────────────────┐
        │                                     │
        │ SUBMISSÃO PARALELA                 │
        │ (Ambas threads começam ao mesmo tempo)│
        │                                     │
        ▼                                     ▼
┌──────────────────────┐      ┌──────────────────────────────┐
│ [2.4] [THREAD 1]      │      │ [2.5] [THREAD 2]              │
│ runnersFuturos.put(  │      │ runnersFuturos.put(            │
│   "GraphQL",         │      │   "DataExport",               │
│   executor.submit(   │      │   executor.submit(             │
│     () ->            │      │     () ->                      │
│       GraphQLRunner   │      │       DataExportRunner         │
│         .executar(   │      │         .executar(             │
│           dataHoje)   │      │           dataHoje)             │
│   )                  │      │   )                            │
│ )                    │      │ )                              │
│ Submete Thread 1     │      │ Submete Thread 2               │
│ (GraphQL)            │      │ (DataExport)                   │
└──────────┬───────────┘      └──────────┬───────────────────┘
           │                              │
           │                              │
           │ ═══════════════════════════════════
           │ AMBAS THREADS COMEÇAM A TRABALHAR
           │ EM PARALELO (SIMULTANEAMENTE)
           │ ═══════════════════════════════════
           │
           ▼                              ▼
           │                              │
           │ [VAI PARA ETAPA 3A]          │ [VAI PARA ETAPA 3B]
           │ (Thread 1 - GraphQL)        │ (Thread 2 - DataExport)
```

---

### ETAPA 3A: EXECUÇÃO PARALELA - RAMO 1 (GraphQL)
**O que faz:** Extrai Coletas e Fretes da API GraphQL em uma thread separada (executa ao mesmo tempo que DataExport)

**⚠️ IMPORTANTE:** Os blocos 3A.7 até 3A.12 detalham especificamente o processo de extração de **Coletas** como exemplo. O mesmo processo (API → Paginação → Transformação → Persistência) se aplica a **Fretes** (bloco 3A.14) e a todas as outras entidades do sistema. Coletas foi escolhida como exemplo porque é uma das principais entidades de negócio e tem todas as características típicas do processo de extração.

**Legenda dos blocos (explicação para leigos):**
- **Bloco 3A.1:** A Thread 1 (GraphQL) foi submetida na Etapa 2 (bloco 2.4) e agora começa a executar. **O que acontece?** O executor Java pega essa thread da fila e começa a executar o código dentro do `executor.submit()`. Ela vai trabalhar sozinha, sem esperar a Thread 2 (DataExport). **Por que trabalha sozinha?** Porque as threads são independentes - cada uma tem seu próprio código para executar e não precisa esperar a outra.

- **Bloco 3A.2:** O `GraphQLRunner.executar(dataHoje)` é chamado. **O que faz?** Este é o método principal do runner GraphQL que recebe a data de hoje como parâmetro e coordena toda a extração da API GraphQL. Ele sabe que precisa buscar dados de hoje (últimas 24 horas).

- **Bloco 3A.3:** O serviço `GraphQLExtractionService.execute()` é chamado. **O que faz?** Este serviço é responsável por organizar e coordenar todas as extrações da API GraphQL. É como um supervisor que dá as instruções: "Primeiro vamos extrair Coletas, depois Fretes". Ele recebe `dataHoje` como data de início e data de fim (porque queremos apenas dados de hoje).

- **Bloco 3A.4:** Antes de começar qualquer extração, faz validações importantes. **O que valida?** Primeiro verifica se consegue conectar no banco de dados usando `CarregadorConfig.validarConexaoBancoDados()`. Se não conseguir, lança um erro e para tudo. Depois verifica se as tabelas necessárias existem no banco usando `CarregadorConfig.validarTabelasEssenciais()`. Se alguma tabela não existir, também lança um erro. **Por que valida antes?** Porque não adianta tentar extrair dados se não conseguir salvá-los depois. É como verificar se a porta está aberta antes de tentar entrar.

- **Bloco 3A.5:** Decide quais entidades vai extrair nesta fase. **O que decide?** Como o parâmetro `entidade` é `null` (não foi especificada uma entidade específica), ele define: `executarColetas = true`, `executarFretes = true`, e `executarFaturasGraphql = false`. **Por que Faturas GraphQL fica para depois?** Porque o processo de enriquecimento de faturas é muito demorado (mais de 50 minutos). Então primeiro extrai as entidades mais rápidas (Coletas e Fretes) e deixa Faturas para a Fase 3 (Etapa 5).

- **Bloco 3A.6:** Extrai **Usuários do Sistema** primeiro. **O que são Usuários do Sistema?** São uma **tabela de dimensão/referência** que contém informações sobre os usuários/motoristas do sistema (ex: ID, nome, etc). É como uma "tabela de cadastro" que outras tabelas referenciam. **Por que extrair primeiro?** Porque as **Coletas têm uma dependência**: cada Coleta possui campos `usuarioId` e `usuarioNome` que referenciam um usuário específico. Se tentarmos salvar uma Coleta com `usuarioId = 123` mas esse usuário ainda não existir no banco, teremos um problema de integridade referencial. **Como funciona?** Chama o `UsuarioSistemaExtractor` que busca TODOS os usuários ativos do sistema (não filtra por data, busca todos com `enabled: true`) da API GraphQL e salva na tabela `usuarios_sistema` do banco. Depois, quando as Coletas forem extraídas, elas já terão os usuários disponíveis para referenciar. É como preparar os ingredientes antes de cozinhar - você precisa ter os usuários "cadastrados" antes de criar registros que referenciam eles.

- **Bloco 3A.7:** Começa a extrair **Coletas**. **O que são Coletas?** Coletas (tipo `Pick` na API GraphQL) são **solicitações de serviço de coleta de carga/mercadorias**. É uma das principais entidades de negócio do sistema de logística. Cada Coleta representa uma solicitação feita por um cliente para que a empresa vá até um local específico buscar uma carga/mercadoria. **O que uma Coleta contém?** Informações como: número da coleta (`sequenceCode`), data/hora da solicitação (`requestDate`, `requestHour`), data/hora do serviço (`serviceDate`, `serviceStartHour`), local de coleta (endereço completo: rua, número, cidade, UF, CEP), cliente que solicitou (`clienteId`, `clienteNome`), usuário/motorista responsável (`usuarioId`, `usuarioNome`), status da coleta (pendente, em andamento, concluída, cancelada), valores (valor total, peso total, número de volumes), filial responsável, e muito mais. **O que faz este bloco?** Chama `ColetaExtractor.extract(dataHoje, dataHoje)` que vai buscar todas as coletas da API GraphQL que foram criadas hoje (últimas 24 horas). Este extractor coordena todo o processo: busca na API, transforma os dados e salva no banco na tabela `coletas`. **Importante:** Agora que os Usuários do Sistema já foram extraídos no bloco 3A.6a, as Coletas podem referenciar corretamente os campos `usuarioId` e `usuarioNome` sem problemas de integridade referencial. Cada Coleta extraída terá seus dados de usuário preenchidos corretamente porque os usuários já existem na tabela `usuarios_sistema`.

- **Bloco 3A.8:** O `ClienteApiGraphQL.buscarColetas()` é chamado. **O que faz?** Este é o cliente HTTP que faz as requisições reais para a API GraphQL buscando especificamente **Coletas** (tipo `Pick`). Ele usa `executarQueryPaginada()` porque os dados vêm em "páginas" (como um livro). A API não retorna todas as coletas de uma vez - retorna de 20 em 20 ou 50 em 50 registros por vez. **O que busca?** Todas as coletas criadas no período especificado (hoje, no caso do fluxo completo) com todos os seus relacionamentos: dados do cliente, endereço de coleta completo, usuário responsável, filial, etc.

- **Bloco 3A.9:** Entra em um loop de paginação para buscar **todas as Coletas**. **Como funciona?** Enquanto `hasNextPage` for `true` (ainda tem mais páginas de coletas), continua pedindo mais dados. A cada iteração: faz uma requisição HTTP para a API GraphQL, recebe uma página de coletas (ex: 50 coletas por página), atualiza o cursor (posição na paginação) e verifica se tem mais páginas. **Por que em loop?** Porque precisa pegar TODAS as coletas do período, não apenas a primeira página. Se houver 500 coletas criadas hoje e a API retorna 50 por página, precisa fazer 10 requisições (10 páginas) para pegar todas. É como ler um livro inteiro, página por página, até chegar no final.

- **Bloco 3A.10:** O `GerenciadorRequisicaoHttp` controla a velocidade e os retries das requisições de **Coletas**. **O que faz?** Primeiro aplica throttling: garante que passa pelo menos 2 segundos entre cada requisição HTTP para buscar coletas (para não sobrecarregar a API GraphQL). Depois, se a requisição der erro, tenta novamente até 5 vezes. **Como trata erros?** Se receber HTTP 429 (Too Many Requests - API está recebendo muitas requisições), espera 2 segundos e tenta de novo. Se receber HTTP 5xx (erro do servidor da API), aplica backoff exponencial (espera 1s, depois 2s, depois 4s, etc) antes de tentar novamente. **Por que isso?** Para respeitar os limites da API GraphQL e ter paciência se algo der errado temporariamente (ex: servidor sobrecarregado, manutenção, etc). Isso garante que mesmo com problemas temporários, todas as coletas sejam extraídas com sucesso.

- **Bloco 3A.11:** Transforma os dados das **Coletas** do formato da API para o formato do banco. **O que transforma?** O `ColetaMapper.toEntity()` converte um objeto `ColetaNodeDTO` (formato JSON que veio da API GraphQL) para um objeto `ColetaEntity` (formato que o banco de dados entende). **O que faz na transformação?** Converte tipos de dados (String para LocalDate nas datas, String para BigDecimal nos valores monetários), expande objetos aninhados (ex: `customer.name` vira `clienteNome`, `pickAddress.city.name` vira `cidadeColeta`), trunca strings muito longas para caber nas colunas do banco, e serializa metadados extras em JSON na coluna `metadata`. É como traduzir de um idioma para outro - pega a estrutura complexa/aninhada da API e transforma em uma estrutura plana/achatada para o banco de dados.

- **Bloco 3A.12:** Salva os dados das **Coletas** no banco de dados. **Como salva?** O `ColetaRepository.salvar()` recebe uma lista de `ColetaEntity` (coletas já transformadas) e para cada uma executa um SQL MERGE (UPSERT) na tabela `coletas`. **O que é MERGE?** É um comando SQL que diz: "Se o registro já existe (mesmo `id`), atualiza todos os campos. Se não existe, cria novo registro (INSERT)." Isso evita duplicatas e sempre mantém os dados atualizados - se uma coleta já foi extraída antes e mudou de status, ela será atualizada, não duplicada. **Como funciona?** Usa transações (batch commits) para salvar vários registros de uma vez (ex: 100 coletas por vez), o que é muito mais eficiente do que salvar uma por uma.

- **Bloco 3A.13:** Aplica um delay de 2 segundos. **Por que espera?** Para não sobrecarregar a API antes de começar a próxima extração (Fretes). É uma pausa de cortesia entre extrações diferentes.

- **Bloco 3A.14:** Agora extrai **Fretes**. **O que são Fretes?** Fretes são os serviços de transporte de carga/mercadorias contratados pelos clientes. É outra entidade principal de negócio do sistema de logística. **O que faz este bloco?** Chama `FreteExtractor.extract(dataHoje, dataHoje)` que segue **exatamente o mesmo processo detalhado das Coletas** (blocos 3A.7 até 3A.12): API → Paginação → Transformação → Persistência. **Por que mesmo processo?** Porque Fretes também vem da mesma API GraphQL e tem a mesma estrutura de paginação. **Nota:** Os detalhes do processo (paginação, retry, transformação, persistência) já foram explicados nos blocos 3A.7 até 3A.12 usando Coletas como exemplo. Fretes segue o mesmo padrão, apenas muda a entidade extraída (Fretes em vez de Coletas) e a tabela de destino (`fretes` em vez de `coletas`).

- **Bloco 3A.15:** Exibe um resumo consolidado. **O que mostra?** Calcula e exibe estatísticas: quantos registros de cada tipo foram extraídos, quantas páginas foram processadas, quanto tempo levou, etc. Isso ajuda a entender o que foi feito.

- **Bloco 3A.16:** Retorna o resultado da thread. **O que retorna?** Se tudo deu certo, retorna normalmente (sem exceção). Se deu algum erro, lança uma exceção. O `Future.get()` na Etapa 4 vai capturar esse resultado (sucesso ou erro) e anotar no contador.

```
┌─────────────────────────────────────────────────────────────┐
│ [3A.1] [THREAD 1 - GraphQL]                                  │
│ runnersFuturos.put("GraphQL",                               │
│   executor.submit(() ->                                     │
│     GraphQLRunner.executar(dataHoje)                        │
│   )                                                         │
│ )                                                           │
│ Submete thread GraphQL para execução paralela               │
└──────────────────────────┬──────────────────────────────────┘
                           │V
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3A.2] [runners.graphql/GraphQLRunner.java]                 │
│ executar(LocalDate dataHoje)                                 │
│ └─ dataHoje = LocalDate.now() (hoje)                        │
│ Runner GraphQL recebe comando e prepara extração             │
└──────────────────────────┬──────────────────────────────────┘
                           │V
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3A.3] [runners.graphql.services/GraphQLExtractionService.java] │
│ execute(dataHoje, dataHoje, null)                            │
│ └─ dataInicio = dataHoje, dataFim = dataHoje                │
│ Serviço de extração GraphQL inicia processamento             │
└──────────────────────────┬──────────────────────────────────┘
                           │V
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3A.4] [VALIDAÇÃO 1]                                        │
│ CarregadorConfig.validarConexaoBancoDados()                 │
│ Valida conexão com banco de dados                           │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
           │ SIM                           │ NÃO
           │ (conectou)                    │ (falhou)
           │                               │
           ▼                               ▼
┌──────────────────────┐      ┌──────────────────────────────┐
│ [3A.4a] [DECISÃO] Conexão válida?                           │
│ Verifica se conseguiu conectar                              │
└──────────┬───────────────────────────────┬──────────────────┘
           │ SIM                           │ NÃO
           │ (conectou)                    │ (falhou)
           │                               │
           ▼                               ▼
┌──────────────────────┐      ┌──────────────────────────────┐
│ [3A.4b] [VALIDAÇÃO 2]│      │ [ERRO] Lançar                 │
│ CarregadorConfig.    │      │ RuntimeException               │
│ validarTabelasEssenciais() │  │ Encerra thread                 │
│ - Obtém lista de tabelas esperadas para a API GraphQL       │
│   (mapa API -> [tabelas])                                    │
│ - Consulta information_schema (ou equivalente)               │
│ - Verifica permissões necessárias (SELECT/INSERT/CREATE)     │
│ - Retorna lista de tabelas ausentes ou sem permissões        │
└──────────┬───────────┘      └──────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3A.4c] [DECISÃO] Tabelas existem?                           │
│ Verifica programaticamente se as tabelas necessárias existem │
│ (usa mapa API -> [tabelas] e checa information_schema)       │
└──────────┬───────────────────────────────┬──────────────────┘
           │ SIM                           │ NÃO
           │ (tabelas OK)                  │ (faltam tabelas)
           │                               │
           ▼                               ▼
┌──────────────────────┐               ┌──────────────────────────────┐
│ [3A.4d] [CONTINUA]   │               │ [AÇÕES EM CASO DE FALTA]     │
│ Prossegue com extração│              │ - Opcional: tentar criar     │
│ das entidades         │              │   tabelas via migração       │
│                      │               │   controlada (lock/transac.) │
│                      │               │ - Ou: Lançar RuntimeException│
│                      │               │   com lista das tabelas falt.│
└──────────────────────┘               └──────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3A.5] [DECISÃO] executarUsuariosSistema == true?            │
│ Verifica se deve extrair tabela de dimensão 'usuarios_sistema'│
└──────────┬───────────────────────────────┬──────────────────┘
           │ SIM                           │ NÃO
           │ (extrair Usuarios)            │ (pular Usuarios)
           │                               │
           ▼                               ▼
┌──────────────────────┐      ┌──────────────────────────────┐
│ [3A.5a] [EXTRAÇÃO]   │      │ [3A.5b] [PULA USUÁRIOS]      │
│ UsuarioSistema       │      │ Pula extração de usuários     │
│ Extractor.extract()  │      │                                │
│ - Busca TODOS usuários│      │                                │
│   ativos do sistema   │      │                                │
│ - Salva na tabela     │      │                                │
│   usuarios_sistema    │      │                                │
└──────────┬───────────┘      └──────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3A.6] [DECISÃO] executarColetas == true?                   │
│ Verifica se deve extrair Coletas                            │
└──────────┬───────────────────────────────┬──────────────────┘
           │ SIM                           │ NÃO
           │ (extrair Coletas)             │ (pular Coletas)
           │                               │
           ▼                               ▼
┌──────────────────────┐      ┌──────────────────────────────┐
│ [3A.6a] [EXTRAÇÃO]   │      │ [3A.6b] [PULA COLETAS]       │
│ ColetaExtractor.extract() │  │ Pula extração de Coletas       │
│ - Executa API → Paginação → Mapper → Repository            │
│ - Salva registros na tabela `coletas`                      │
└──────────┬───────────┘      └──────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────┐
│ [3A.7] [runners.graphql.extractors/ColetaExtractor.java]     │
│ extract(dataHoje, dataHoje)                                 │
│ └─ apiClient.buscarColetas(dataHoje, dataHoje)              │
│                                                              │
│ O que extrai:                                                │
│ - Solicitações de coleta de carga/mercadorias                │
│ - Tipo "Pick" na API GraphQL                                │
│ - Dados: número, datas, local, cliente, usuário,             │
│   status, valores, pesos, volumes, etc                        │
│                                                              │
│ Salva na tabela: coletas                                     │
│ (Só executa se executarColetas == true)                      │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3A.8] [api/ClienteApiGraphQL.java]                         │
│ buscarColetas(dataHoje, dataHoje)                            │
│ └─ executarQueryPaginada(query, "coletas", variaveis,       │
│    ColetaNodeDTO.class)                                      │
│ Cliente API GraphQL faz requisições paginadas                │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3A.9] [LOOP DE PAGINAÇÃO]                                  │
│ while (hasNextPage) {                                        │
│                                                              │
│   ┌──────────────────────────────────────────────────────┐ │
│   │ [3A.9a] [APLICAR THROTTLING] ◄───────────────────────┼──┐
│   │ GerenciadorRequisicaoHttp.aplicarThrottling()         │ │ │
│   │ └─ Garante 2s mínimo entre requisições                │ │ │
│   │ Aplica controle de velocidade                          │ │ │
│   └──────────────────────────┬───────────────────────────┘ │
│                              │                               │
│                              ▼                               │
│   ┌──────────────────────────────────────────────────────┐ │
│   │ [3A.10] [LOOP RETRY - até 5 tentativas]              │ │
│   │ for (tentativa = 1; tentativa <= 5) {                │ │
│   │                                                         │ │
│   │   ┌─────────────────────────────────────────────────┐ │ │
│   │   │ [3A.10a] [EXECUTAR REQUISIÇÃO]                 │ │ │
│   │   │ cliente.send(requisicao)                        │ │ │
│   │   │ Faz requisição HTTP para API                    │ │ │
│   │   └──────────────────┬──────────────────────────────┘ │ │
│   │                      │                                 │ │
│   │                      ▼                                 │ │
│   │   ┌─────────────────────────────────────────────────┐ │ │
│   │   │ [3A.10b] [DECISÃO] statusCode >= 200 && < 300? │ │ │
│   │   │ Verifica se requisição foi bem-sucedida          │ │ │
│   │   └──────────┬───────────────────┬──────────────────┘ │ │
│   │              │ SIM               │ NÃO                │ │
│   │              │ (sucesso)         │ (erro)             │ │
│   │              │                    │                    │ │
│   │              ▼                    ▼                    │ │
│   │   ┌──────────────────┐  ┌──────────────────────────┐ │ │
│   │   │ [SUCESSO]        │  │ [3A.10c] [DECISÃO]        │ │ │
│   │   │ Retornar resposta│  │ statusCode == 429?        │ │ │
│   │   │ Sair do loop     │  │ (Too Many Requests)       │ │ │
│   │   └──────────────────┘  └──────────┬───────────────┘ │ │
│   │                                     │                  │ │
│   │                                     │ SIM              │ │
│   │                                     │ (429)            │ │
│   │                                     │                  │ │
│   │                                     ▼                  │ │
│   │                          ┌──────────────────────────┐ │ │
│   │                          │ [3A.10d] Aguardar 2s     │ │ │
│   │                          │ Thread.sleep(2000)        │ │ │
│   │                          │ Continuar loop retry      │ │ │
│   │                          └──────────┬───────────────┘ │ │
│   │                                     │                  │ │
│   │                                     │ NÃO              │ │
│   │                                     │ (não é 429)      │ │
│   │                                     │                  │ │
│   │                                     ▼                  │ │
│   │                          ┌──────────────────────────┐ │ │
│   │                          │ [3A.10e] [DECISÃO]        │ │ │
│   │                          │ statusCode >= 500?        │ │ │
│   │                          │ (Erro do servidor)        │ │ │
│   │                          └──────────┬───────────────┘ │ │
│   │                                     │                  │ │
│   │                                     │ SIM              │ │
│   │                                     │ (5xx)            │ │
│   │                                     │                  │ │
│   │                                     ▼                  │ │
│   │                          ┌──────────────────────────┐ │ │
│   │                          │ [3A.10f] Backoff         │ │ │
│   │                          │ exponencial               │ │ │
│   │                          │ Aguardar tempo crescente  │ │ │
│   │                          │ Continuar loop retry       │ │ │
│   │                          └──────────────────────────┘ │ │
│   │                                                         │ │
│   │   } // Fim do loop retry                               │ │
│   └──────────────────────────────────────────────────────┘ │
│   │                                                         │
│   │ (Após retry bem-sucedido, temos a resposta HTTP)        │
│   │                                                         │
│   │                                                         │
│   ▼                                                         │
│   ┌──────────────────────────────────────────────────────┐ │
│   │ [3A.9b] [DESERIALIZAÇÃO]                             │ │
│   │ [api/ClienteApiGraphQL.java]                          │ │
│   │ mapeadorJson.treeToValue(node, ColetaNodeDTO.class)   │ │
│   │                                                         │ │
│   │ Converte JSON da resposta HTTP em objetos Java         │ │
│   │ List<ColetaNodeDTO>                                    │ │
│   │                                                         │ │
│   │ Processa estrutura edges/node do GraphQL               │ │
│   └──────────────────────────┬─────────────────────────────┘ │
│                              │                               │
│                              ▼                               │
│   ┌──────────────────────────────────────────────────────┐ │
│   │ [3A.9c] [ATUALIZAÇÃO PAGINAÇÃO]                      │ │
│   │ [api/ClienteApiGraphQL.java]                          │ │
│   │ cursor = resposta.getEndCursor()                      │ │
│   │ hasNextPage = resposta.hasNextPage()                 │ │
│   │                                                         │ │
│   │ Atualiza cursor para próxima página                    │ │
│   │ Verifica se há mais páginas                            │ │
│   └──────────────────────────┬─────────────────────────────┘ │
│                              │                               │
│                              ▼                               │
│   ┌──────────────────────────────────────────────────────┐ │
│   │ [3A.9d] [DECISÃO] hasNextPage == true?                │ │
│   │ Verifica se há mais páginas para buscar                │ │
│   └──────────┬───────────────────┬─────────────────────────┘ │
│              │ SIM               │ NÃO                        │
│              │ (há mais páginas) │ (última página)            │
│              │                   │                            │
│              ▼                   ▼                            │
│              │                   │                            │
│              │                   ┌──────────────────────────┐ │
│              │                   │ [FIM DO LOOP]            │ │
│              │                   │ Sair do while (hasNextPage)│ │
│              │                   │ Todas as páginas processadas│ │
│              │                   └──────────┬───────────────┘ │
│              │                              │                  │
│              │                              ▼                  │
│              │                              │                  │
│              │                              │ (Sai do loop)    │
│              │                              │                  │
│              │                              │ (Vai para RETORNO)│
│              │                              │                  │
│              │ (Volta para 3A.9a)           │                  │
│              │                              │                  │
│              └──────────────────────────────┼──────────────────┘
│                                             │
│                                             │ (Após sair do loop)
│                                             │
│                                             └─────────────────────┐
│                                                                 │
│   } // Fim do loop while (hasNextPage)                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
   │
   │ (Todas as páginas foram coletadas - List<ColetaNodeDTO>)
   │
   ▼
┌─────────────────────────────────────────────────────────────┐
│ [RETORNO] ResultadoExtracao.completo(todasColetas)          │
│ Retorna todas as coletas coletadas de todas as páginas        │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3A.11] [TRANSFORMAÇÃO]                                     │
│ [modelo.graphql.coletas/ColetaMapper.java]                  │
│ toEntity(ColetaNodeDTO dto)                                 │
│ └─ Converter tipos, truncar strings, serializar metadata   │
│ Converte dados da API para formato do banco                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3A.12] [PERSISTÊNCIA]                                      │
│ [db.repository/ColetaRepository.java]                       │
│ salvar(List<ColetaEntity> entities)                         │
│ └─ [HERDA DE] AbstractRepository.salvar()                   │
│    └─ [LOOP] Para cada entidade:                            │
│                                                             │
│       ┌──────────────────────────────────────────────────┐  │
│       │ [3A.12a] [EXECUTAR MERGE]                        │  │
│       │ executarMerge(conexao, entidade)                 │  │
│       │ └─ SQL MERGE (UPSERT)                            │  │
│       │ Executa comando SQL MERGE                        │  │
│       └──────────────────┬───────────────────────────────┘  │
│                          │                                  │
│                          ▼                                  │
│       ┌──────────────────────────────────────────────────┐  │
│       │ [3A.12b] [DECISÃO] Registro existe?              │  │
│       │ Verifica se MERGE fez UPDATE ou INSERT           │  │
│       └──────────┬───────────────────┬───────────────── ─┘  │
│                  │ SIM               │ NÃO                  │
│                  │ (UPDATE)          │ (INSERT)             │
│                  │                   │                      │
│                  ▼                   ▼                      │
│       ┌──────────────────┐  ┌──────────────────────────┐    │
│       │ [UPDATE]        │  │ [INSERT]                  │    │
│       │ Registro        │  │ Registro                  │    │
│       │ atualizado      │  │ criado                    │    │
│       └──────────────────┘  └──────────────────────────┘    │
│                                                             │
│    } // Fim do loop para cada entidade                      │
│                                                             │
│ Salva dados no banco (INSERT ou UPDATE via MERGE)           │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           │ CAMINHO 1 (executarColetas=true)
                           │ Sai de: 3A.12 (Persistência de Coletas)
                           │
                           │
                           │
                           │                    CAMINHO 2 (executarColetas=false)
                           │                    Sai de: 3A.6b (Pula Coletas)
                           │                    ────────────────────────────────┐
                           │                                                      │
                           │                                                      │
                           └──────────────────────────────────────────────────────┘
                                                                 │
                                                                 │ (Ambos convergem aqui)
                                                                 │
                                                                 ▼
┌─────────────────────────────────────────────────────────────┐
│ [CONVERGÊNCIA DOS CAMINHOS]                                │
│                                                              │
│ Condição original (3A.6): executarColetas == true?          │
│                                                              │
│ ┌────────────────────────────────────────────────────────┐  │
│ │ CAMINHO 1 (SIM - executarColetas=true):               │  │
│ │ Origem: Sai de 3A.12 (Persistência de Coletas)         │  │
│ │                                                         │  │
│ │ Processou:                                              │  │
│ │ • 3A.6a: Extrai Usuários                               │  │
│ │ • 3A.7: Extrai Coletas                                 │  │
│ │ • 3A.8 → 3A.9 → 3A.10 → 3A.11 → 3A.12                │  │
│ │   (Paginação → Transformação → Persistência)          │  │
│ │                                                         │  │
│ │ Chega aqui: Após salvar todas as Coletas no banco      │  │
│ └────────────────────────────────────────────────────────┘  │
│                                                              │
│ ┌────────────────────────────────────────────────────────┐  │
│ │ CAMINHO 2 (NÃO - executarColetas=false):              │  │
│ │ Origem: Sai de 3A.6b (Pula Coletas)                     │  │
│ │                                                         │  │
│ │ Pulou:                                                  │  │
│ │ • 3A.6a (Usuários) - opcional, depende de config       │  │
│ │ • 3A.7, 3A.8, 3A.9, 3A.10, 3A.11, 3A.12 (Coletas)     │  │
│ │                                                         │  │
│ │ Chega aqui: Direto, sem processar Coletas              │  │
│ └────────────────────────────────────────────────────────┘  │
│                                                              │
│ ═══════════════════════════════════════════════════════════ │
│ CONVERGÊNCIA: Ambos caminhos chegam no 3A.13 (Delay)        │
│ Depois seguem juntos para 3A.14 (Fretes)                    │
│ ═══════════════════════════════════════════════════════════ │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3A.13] [APLICAR DELAY]                                     │
│ ExtractionHelper.aplicarDelay() (2 segundos)                 │
│ Aguarda 2 segundos antes da próxima extração                 │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3A.14] [EXTRAÇÃO DE FRETES]                                │
│ [runners.graphql.extractors/FreteExtractor.java]            │
│ extract(dataHoje, dataHoje)                                 │
│                                                              │
│ ⚠️ NOTA: Segue o MESMO processo detalhado das Coletas        │
│ (blocos 3A.7 até 3A.12)                                     │
│                                                              │
│ Fluxo: API → Paginação → Transformação → Persistência      │
│                                                              │
│ Diferenças em relação a Coletas:                            │
│ - Extractor: FreteExtractor (em vez de ColetaExtractor)    │
│ - API: buscarFretes() (em vez de buscarColetas())          │
│ - Mapper: FreteMapper (em vez de ColetaMapper)             │
│ - Repository: FreteRepository (em vez de ColetaRepository) │
│ - Tabela: fretes (em vez de coletas)                        │
│                                                              │
│ O processo de paginação, retry, transformação e             │
│ persistência é idêntico ao das Coletas.                     │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3A.15] [RESUMO GRAPHQL]                                    │
│ exibirResumoConsolidado(resultados, inicioExecucao)          │
│ └─ Calcular estatísticas, log resumo                        │
│ Exibe resumo da extração GraphQL                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3A.16] [RETORNO THREAD 1]                                  │
│ Future.get() → Sucesso ou Exception                          │
│ Retorna resultado da thread (sucesso ou erro)                │
└──────────────────────────┬──────────────────────────────────┘
```

---

### ETAPA 3B: EXECUÇÃO PARALELA - RAMO 2 (DataExport)
**O que faz:** Extrai Manifestos, Cotações e outras entidades da API Data Export em outra thread (executa ao mesmo tempo que GraphQL)

**Legenda dos blocos (explicação para leigos):**
- **Bloco 3B.1:** A Thread 2 (DataExport) foi submetida na Etapa 2 (bloco 2.5) e agora começa a executar. **O que acontece?** O executor Java pega essa thread da fila e começa a executar o código dentro do `executor.submit()`. Ela trabalha ao mesmo tempo que a Thread 1 (GraphQL), mas em uma "loja" diferente (API diferente). **Por que ao mesmo tempo?** Porque as duas APIs são independentes - uma não depende da outra. Então faz sentido extrair ambas simultaneamente para economizar tempo total.

- **Bloco 3B.2:** O `DataExportRunner.executar(dataHoje)` é chamado. **O que faz?** Este é o método principal do runner DataExport que recebe a data de hoje como parâmetro e coordena toda a extração da API Data Export. Ele também sabe que precisa buscar dados de hoje (últimas 24 horas).

- **Bloco 3B.3:** O serviço `DataExportExtractionService.execute()` é chamado. **O que faz?** Este serviço é responsável por organizar e coordenar todas as extrações da API Data Export. É como um supervisor que dá as instruções: "Vamos extrair Manifestos, depois Cotações, depois Localização de Carga, etc". Ele também recebe `dataHoje` como data de início e data de fim.

- **Bloco 3B.4:** Antes de começar qualquer extração, faz as mesmas validações da Thread 1. **O que valida?** Primeiro verifica se consegue conectar no banco de dados. Se não conseguir, lança um erro e para tudo. Depois verifica se as tabelas necessárias existem no banco. **Por que valida de novo?** Porque cada thread é independente e precisa garantir que tem acesso ao banco antes de começar. É uma verificação de segurança.

- **Bloco 3B.5:** Faz um trabalho sequencial (uma coisa depois da outra). **Por que sequencial?** Porque a API Data Export tem várias entidades diferentes e cada uma precisa ser extraída completamente antes de começar a próxima. **O que extrai, em ordem?** 1) Manifestos (extrai todos, salva no banco), espera 2s, 2) Cotações (extrai todos, salva no banco), espera 2s, 3) Localização de Carga (extrai todos, salva no banco), espera 2s, 4) Contas a Pagar (extrai todos, salva no banco), espera 2s, 5) Faturas por Cliente (extrai todos, salva no banco). **Cada uma segue o mesmo processo:** API → Paginação (se necessário) → Transformação (Mapper) → Persistência (Repository). **Por que espera 2s entre cada?** Para não sobrecarregar a API Data Export com requisições muito rápidas.

- **Bloco 3B.6:** Exibe um resumo consolidado. **O que mostra?** Calcula e exibe estatísticas de todas as 5 entidades extraídas: quantos registros de cada tipo foram extraídos, quantas páginas foram processadas (se aplicável), quanto tempo levou cada extração, etc. Isso ajuda a entender o que foi feito pelo Trabalhador 2.

- **Bloco 3B.7:** Retorna o resultado da thread. **O que retorna?** Se tudo deu certo, retorna normalmente (sem exceção). Se deu algum erro em qualquer uma das 5 extrações, lança uma exceção. O `Future.get()` na Etapa 4 vai capturar esse resultado (sucesso ou erro) e anotar no contador. **Importante:** A Thread 2 pode terminar antes ou depois da Thread 1 - não importa a ordem, ambas trabalham independentemente.

```
┌─────────────────────────────────────────────────────────────┐
│ [3B.1] [THREAD 2 - DataExport]                               │
│ runnersFuturos.put("DataExport",                             │
│   executor.submit(() ->                                     │
│     DataExportRunner.executar(dataHoje)                      │
│   )                                                         │
│ )                                                           │
│ Submete thread DataExport para execução paralela             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3B.2] [runners.dataexport/DataExportRunner.java]          │
│ executar(LocalDate dataHoje)                                │
│ └─ dataHoje = LocalDate.now() (hoje)                        │
│ Runner DataExport recebe comando e prepara extração         │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3B.3] [runners.dataexport.services/DataExportExtractionService.java] │
│ execute(dataHoje, dataHoje, null)                            │
│ └─ dataInicio = dataHoje, dataFim = dataHoje                │
│ Serviço de extração DataExport inicia processamento         │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3B.4] [VALIDAÇÃO 1]                                        │
│ CarregadorConfig.validarConexaoBancoDados()                 │
│ Valida conexão com banco de dados                           │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3B.4a] [DECISÃO] Conexão válida?                           │
│ Verifica se conseguiu conectar                              │
└──────────┬───────────────────────────────┬──────────────────┘
           │ SIM                           │ NÃO
           │ (conectou)                    │ (falhou)
           │                               │
           ▼                               ▼
┌──────────────────────┐      ┌──────────────────────────────┐
│ [3B.4b] [VALIDAÇÃO 2]│      │ [ERRO] Lançar                 │
│ CarregadorConfig.    │      │ RuntimeException               │
│ validarTabelas       │      │ Encerra thread                 │
│ Essenciais()         │      │                               │
│ - Obtém lista de     │      │                               │
│   tabelas esperadas  │      │                               │
│   para a API DataExport│    │                               │
│ - Consulta           │      │                               │
│   information_schema │      │                               │
│   (ou equivalente)   │      │                               │
│ - Verifica permissões│      │                               │
│   necessárias (SEL/  │      │                               │
│   INS/CREATE)        │      │                               │
│ - Retorna lista de   │      │                               │
│   tabelas ausentes   │      │                               │
└──────────┬───────────┘      └──────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3B.4c] [DECISÃO] Tabelas existem?                          │
│ Verifica programaticamente se as tabelas necessárias existem│
│ (usa mapa API -> [tabelas] e checa information_schema)       │
└──────────┬───────────────────────────────┬──────────────────┘
           │ SIM                           │ NÃO
           │ (tabelas OK)                  │ (faltam tabelas)
           │                               │
           ▼                               ▼
┌──────────────────────┐               ┌──────────────────────────────┐
│ [3B.4d] [CONTINUA]   │               │ [AÇÕES EM CASO DE FALTA]     │
│ Prossegue com extração│              │ - Opcional: tentar criar     │
│ sequencial das entidades│            │   tabelas via migração       │
│                      │               │   controlada (lock/transac.) │
│                      │               │ - Ou: Lançar RuntimeException│
│                      │               │   com lista das tabelas falt.│
└──────────────────────┘               └──────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3B.5] [LOOP SEQUENCIAL - 5 ENTIDADES]                     │
│                                                              │
│ 1. ManifestoExtractor → Extrai Manifestos                   │
│ 2. Delay 2s                                                 │
│ 3. CotacaoExtractor → Extrai Cotações                       │
│ 4. Delay 2s                                                 │
│ 5. LocalizacaoCargaExtractor → Extrai Localização           │
│ 6. Delay 2s                                                 │
│ 7. ContasAPagarExtractor → Extrai Contas a Pagar           │
│ 8. Delay 2s                                                 │
│ 9. FaturaPorClienteExtractor → Extrai Faturas por Cliente  │
│                                                              │
│ Cada uma segue: API → Paginação → Mapper → Repository      │
│ Loop sequencial extrai 5 entidades                          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3B.6] [RESUMO DATA EXPORT]                                 │
│ exibirResumoConsolidado(resultados, inicioExecucao)          │
│ Exibe resumo da extração DataExport                          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [3B.7] [RETORNO THREAD 2]                                   │
│ Future.get() → Sucesso ou Exception                          │
│ Retorna resultado da thread (sucesso ou erro)                │
└──────────────────────────┬──────────────────────────────────┘
```

---

### ETAPA 4: CONVERGÊNCIA - AGUARDAR AMBAS THREADS
**O que faz:** Aguarda as duas threads terminarem, verifica se houve erros e prepara para a próxima fase

**Legenda dos blocos (explicação para leigos):**
- **Bloco 4.1:** O Trabalhador 1 (GraphQL) termina seu trabalho. **O que acontece?** A Thread 1 completa a execução do código (terminou a Etapa 3A) e retorna. Se tudo deu certo, retorna normalmente. Se deu algum erro, lança uma exceção. O código principal chama `futuroGraphQL.get()` que é um método bloqueante - ele fica esperando até a thread terminar antes de continuar. **O que é bloqueante?** Significa que o código principal para e espera a thread terminar antes de continuar. Quando a thread termina, o `get()` retorna (se sucesso) ou lança uma exceção (se erro).

- **Bloco 4.2:** O Trabalhador 2 (DataExport) termina seu trabalho. **O que acontece?** A Thread 2 completa a execução do código (terminou a Etapa 3B) e retorna. O código principal chama `futuroDataExport.get()` que também é bloqueante. **Importante:** A Thread 2 pode terminar antes ou depois da Thread 1 - não importa a ordem. O `get()` vai esperar cada uma terminar independentemente. Se a Thread 1 terminar primeiro, o código espera a Thread 2. Se a Thread 2 terminar primeiro, o código espera a Thread 1.

- **Bloco 4.3:** O supervisor (código principal) verifica o resultado de cada trabalhador. **Como verifica?** Faz um loop (`for`) sobre o mapa `runnersFuturos` que contém as referências de ambas as threads. Para cada thread, tenta fazer `futuro.get()`. **O que acontece?** Se `get()` retornar normalmente (sem exceção), incrementa `totalSucessos++` e registra que aquela API foi concluída com sucesso. Se `get()` lançar uma `ExecutionException` ou `InterruptedException`, incrementa `totalFalhas++`, adiciona o nome da API na lista `runnersFalhados`, e registra o erro no log. **Por que continua mesmo com erro?** Porque o sistema foi projetado para ser resiliente - se uma API falhar, a outra ainda pode ter sucesso e os dados dessa API serão preservados.

- **Bloco 4.4:** Encerra o "gerenciador de tarefas paralelas" (ExecutorService). **O que faz?** Chama `executor.shutdown()` que diz ao executor: "Não aceite mais tarefas novas e espere as que estão rodando terminarem, depois encerre". **Por que encerra?** Porque já não precisa mais - ambas as threads terminaram. **Importante:** Independente de ter havido falhas ou não, o sistema continua para a Fase 3 (Etapa 5) para extrair Faturas GraphQL. Isso garante que mesmo se alguma API principal falhar, ainda tenta extrair as faturas.

```
        ┌─────────────────────────────────────┐
        │                                     │
        │ [4.1] THREAD 1 (GraphQL)            │
        │ Thread 1 termina e retorna resultado │
        │ (aguardando get() no loop 4.3)      │
        │                                     │
        └──────────────┬──────────────────────┘
                       │
                       │ (executando em paralelo)
                       │
        ┌──────────────┴──────────────────────┐
        │                                     │
        │ [4.2] THREAD 2 (DataExport)          │
        │ Thread 2 termina e retorna resultado │
        │ (aguardando get() no loop 4.3)      │
        │                                     │
        └──────────────┬──────────────────────┘
                       │
                       │ (ambas threads terminam)
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ [4.3] [comandos.extracao/ExecutarFluxoCompletoComando.java] │
│ [LOOP] Para cada entry em runnersFuturos:                  │
│ ┌──────────────────────────────────────────────────────┐  │
│ │ [4.3a] future.get() (bloqueante)                      │  │
│ │ └─ Aguarda thread terminar e obtém resultado          │  │
│ │                                                         │  │
│ │ [4.3b] [DECISÃO] Exception?                           │  │
│ │ ├─ ExecutionException? → totalFalhas++                │  │
│ │ ├─ InterruptedException? → totalFalhas++             │  │
│ │ └─ Sucesso? → totalSucessos++                         │  │
│ └──────────────────────────────────────────────────────┘  │
│                                                              │
│ executor.shutdown()                                         │
│ Verifica resultado de cada thread (sucesso ou erro)          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [4.4] [CONTINUAÇÃO]                                         │
│ └─ Independente de falhas, continua para Fase 3            │
│ Encerra executor e continua para próxima fase               │
└──────────────────────────┬──────────────────────────────────┘
```

---

### ETAPA 5: FASE 3 - FATURAS GRAPHQL (SEQUENCIAL)
**O que faz:** Extrai Faturas GraphQL por último (processo demorado, executa sozinho após as outras entidades)

**Legenda dos blocos (explicação para leigos):**
- **Bloco 5.1:** Agora que as outras entidades já foram extraídas (Coletas, Fretes, Manifestos, Cotações, etc), inicia a extração de Faturas GraphQL. **Por que fica por último?** Porque o processo de enriquecimento de faturas é muito demorado (mais de 50 minutos). Se fizesse primeiro, as outras entidades só seriam extraídas depois de mais de 50 minutos, o que atrasaria todo o processo. **Estratégia:** Extrai primeiro as entidades rápidas para garantir que o BI (Business Intelligence) tenha dados atualizados rapidamente, e deixa as faturas (que são mais complexas) para depois.

- **Bloco 5.2:** Chama `GraphQLRunner.executarFaturasGraphQLPorIntervalo(dataHoje, dataHoje)`. **O que faz?** Este método específico extrai apenas Faturas GraphQL. **Por que método diferente?** Porque faturas têm um processo especial de enriquecimento que é diferente das outras entidades. **Importante:** Dessa vez não usa threads paralelas - executa sequencialmente (sozinho) porque é um processo muito pesado e não precisa competir com outras extrações.

- **Bloco 5.3:** O `FaturaGraphQLExtractor.extract()` processa as Faturas GraphQL. **Por que é demorado?** Porque cada fatura precisa ser "enriquecida" com informações adicionais. O que isso significa? Para cada fatura, o sistema precisa fazer requisições adicionais à API para buscar detalhes complementares (itens da fatura, informações do cliente, etc). É como fazer um trabalho muito detalhado que leva muito tempo. **Processo:** Segue o mesmo fluxo das outras entidades (API → Paginação → Transformação → Persistência), mas com muitas requisições extras para enriquecimento.

- **Bloco 5.4:** Verifica se a extração de Faturas foi bem-sucedida ou se deu algum erro. **Como verifica?** Usa um bloco `try-catch`. Se o método `executarFaturasGraphQLPorIntervalo()` executar sem lançar exceção, incrementa `totalSucessos++` e registra sucesso. Se lançar uma exceção, incrementa `totalFalhas++`, adiciona "FaturasGraphQL" na lista `runnersFalhados`, e registra o erro no log. **Importante:** Mesmo se falhar, o sistema continua para a Etapa 6 (Validação) porque as outras entidades já foram extraídas com sucesso.

```
┌─────────────────────────────────────────────────────────────┐
│ [5.1] [comandos.extracao/ExecutarFluxoCompletoComando.java] │
│ [FASE 3] Executar Faturas GraphQL por último                │
│ GraphQLRunner.executarFaturasGraphQLPorIntervalo(            │
│   dataHoje, dataHoje)                                        │
│ Inicia extração de Faturas GraphQL (Fase 3)                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [5.2] [runners.graphql/GraphQLRunner.java]                  │
│ executarFaturasGraphQLPorIntervalo(dataHoje, dataHoje)      │
│ └─ service.execute(dataHoje, dataHoje, FATURAS_GRAPHQL)    │
│ Runner GraphQL executa extração de Faturas                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [5.3] [runners.graphql.extractors/FaturaGraphQLExtractor.java] │
│ extract(dataHoje, dataHoje)                                 │
│ └─ [MESMO FLUXO: API → Mapper → Repository]                │
│    └─ [OBSERVAÇÃO] Processo demorado (50+ minutos)         │
│ Extractor de Faturas GraphQL processa dados (50+ minutos)    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [5.4] [DECISÃO] Exception?                                   │
│ ├─ SIM → totalFalhas++, runnersFalhados.add()               │
│ └─ NÃO → totalSucessos++                                    │
│ Verifica se extração foi bem-sucedida                        │
└──────────────────────────┬──────────────────────────────────┘
```

---

### ETAPA 6: VALIDAÇÃO DE COMPLETUDE
**O que faz:** Compara quantos registros foram extraídos com quantos existem na API, verifica se está tudo completo

**Legenda dos blocos (explicação para leigos):**
- **Bloco 6.1:** Cria um "fiscal" (validador) que vai verificar se tudo foi extraído corretamente. **O que faz?** Instancia um objeto `CompletudeValidator` que é responsável por comparar os dados extraídos com os dados que existem na API. É como um inspetor de qualidade que verifica se o trabalho foi feito completamente.

- **Bloco 6.2:** Pergunta para a API: "Quantos registros você tem de cada tipo?" **Como pergunta?** Chama `validator.buscarTotaisEslCloud(dataReferencia)` que faz requisições especiais para a API que retornam apenas a contagem total de registros (não os dados em si, só os números). **O que faz com os números?** Guarda em um objeto `Optional<TotaisEslCloud>`. **E se a API não responder?** Se a API não responder ou der erro, o `Optional` fica vazio (`isPresent() == false`). Nesse caso, pula a validação completamente e vai direto para a finalização. **Por que pula?** Porque não consegue validar sem saber quantos registros deveriam existir. É melhor continuar do que travar o processo.

- **Bloco 6.3:** Compara os números da API com os números do banco. **Como compara?** Chama `validator.validarCompletude(totaisEslCloud, dataReferencia)`. Para cada entidade (Coletas, Fretes, Manifestos, etc): 1) Busca o total da API (quantos registros a API disse que tem), 2) Busca o total do banco usando `SELECT COUNT(*) FROM tabela`, 3) Compara os dois números. **O que significa cada resultado?** Se API == Banco: OK (extração completa). Se API > Banco: INCOMPLETO (faltam registros). Se API < Banco: DUPLICADOS (tem mais registros no banco do que deveria). **Exemplo:** "A API disse que tem 1000 Coletas. Quantas temos no banco? 1000? Perfeito! 950? Faltam 50! 1050? Tem duplicados!"

- **Bloco 6.4:** Se tudo está completo (todos os status == OK), faz validações extras. **O que valida?** Primeiro valida gaps: `validator.validarGapsOcorrencias()` verifica se não faltam IDs sequenciais no banco (ex: tem ID 1, 2, 4, 5 mas falta o 3). Depois valida janela temporal: `validator.validarJanelaTemporal()` verifica se não foram criados novos registros na API durante o tempo da extração (o que poderia causar inconsistências). **Por que valida só se completo?** Porque se já está incompleto, não faz sentido validar detalhes - já sabemos que tem problema.

- **Bloco 6.5:** Se não está completo (algum status != OK), pula as validações extras. **O que faz?** Apenas registra no log que "Incompletude detectada" com os detalhes de quais entidades estão incompletas ou têm duplicados. Não faz validações de gaps ou janela temporal porque já sabemos que tem problema maior.

- **Bloco 6.6:** Independente do resultado da validação (completo ou incompleto, com ou sem validações extras), continua para a finalização do processo. **Por que continua?** Porque a validação é apenas informativa - ela não impede o processo de continuar. Os dados já foram extraídos (mesmo que incompletos) e o sistema precisa finalizar normalmente, exibindo os resultados e salvando os logs.

```
┌─────────────────────────────────────────────────────────────┐
│ [6.1] [comandos.extracao/ExecutarFluxoCompletoComando.java] │
│ [VALIDAÇÃO]                                                 │
│ CompletudeValidator validator = new CompletudeValidator()   │
│ Cria validador de completude                                │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [6.2] [auditoria.servicos/CompletudeValidator.java]         │
│ buscarTotaisEslCloud(dataReferencia)                        │
│ Busca totais das APIs (quantos registros existem)           │
│ Retorna: Optional<TotaisEslCloud>                           │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [6.2a] [DECISÃO] Optional.isPresent()?                      │
│ Verifica se conseguiu buscar totais da API                  │
└──────────┬───────────────────────────────┬──────────────────┘
           │ SIM                           │ NÃO
           │ (conseguiu buscar)            │ (API não respondeu)
           │                               │
           ▼                               ▼
           │                      ┌──────────────────────────────┐
           │                      │ [6.2b] [PULA VALIDAÇÃO]       │
           │                      │ Log: "Continuando sem         │
           │                      │        validação"             │
           │                      │ Vai direto para finalização  │
           │                      └──────────┬───────────────────┘
           │                                  │
           │                                  │
           │                                  │
           ▼                                  │
┌─────────────────────────────────────────────────────────────┐
│ [6.3] [auditoria.servicos/CompletudeValidator.java]         │
│ validarCompletude(totaisEslCloud, dataReferencia)           │
│ └─ [SÓ EXECUTA SE Optional.isPresent() == true]             │
│ ┌──────────────────────────────────────────────────────┐    │
│ │ [LOOP] Para cada entidade:                           │    │
│ │ ┌──────────────────────────────────────────────────┐ │    │
│ │ │ [BUSCAR TOTAL API] → Obter contagem da API       │ │    │
│ │ │ [BUSCAR TOTAL BANCO] → SELECT COUNT(*)           │ │    │
│ │ │ [COMPARAR] API == Banco?                         │ │    │
│ │ │ ├─ SIM → OK                                      │ │    │
│ │ │ └─ NÃO → INCOMPLETO ou DUPLICADOS                │ │    │
│ │ └──────────────────────────────────────────────────┘ │    │
│ └──────────────────────────────────────────────────────┘    │
│ Compara totais da API com totais do banco                   │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           │
                           │                    (6.2b - pulou validação)
                           │                    ────────────────────────┐
                           │                                              │
                           │                                              │
                           ▼                                              │
┌─────────────────────────────────────────────────────────────┐
│ [6.4] [DECISÃO] extracaoCompleta == true?                   │
│ (Todos status == OK)                                        │
└──────────┬───────────────────────────────┬──────────────────┘
           │ SIM                           │ NÃO
           │ (completo)                    │ (incompleto)
           │                               │
           ▼                               ▼
┌───────────────────────┐      ┌─────────────────────────────┐
│ [6.4a] VALIDA GAPS    │      │ [6.5] [PULA VALIDAÇÕES]     │
│ validator.validarGaps │      │ └─ Log: "Incompletude       │
│ Ocorrencias()         │      │    detectada"               │
│                       │      │                             │
│ VALIDA TEMPORAL       │      │                             │
│ validator.validar     │      │                             │
│ JanelaTemporal()      │      │                             │
│                       │      │                             │
│ Log resultado         │      │                             │
│ Se completo, valida gaps e janela temporal                 │
└──────────┬────────────┘      └──────────┬──────────────────┘
           │                              │
           │                              │
           └──────────────┬───────────────┘
                          │
                          │                    (6.2b - pulou validação)
                          │                    ────────────────────────┐
                          │                                              │
                          │                                              │
                          │ ═══════════════════════════════════════════════════
                          │ CONVERGÊNCIA: Todos os caminhos chegam aqui:
                          │ - 6.2b: API não respondeu, pulou validação
                          │ - 6.4a: Validação completa, validou gaps/temporal
                          │ - 6.5: Validação incompleta, pulou validações extras
                          │ Independente do caminho, o processo continua
                          │ para a finalização.
                          │ ═══════════════════════════════════════════════════
                          │                                              │
                          │                                              │
                          └──────────────────────────────────────────────┘
                                                                 │
                                                                 ▼
┌─────────────────────────────────────────────────────────────┐
│ [6.6] [CONTINUA PARA FINALIZAÇÃO]                           │
│ Continua para finalização                                    │
│ (Todos os caminhos convergem aqui)                           │
└─────────────────────────────────────────────────────────────┘
```

---

### ETAPA 7: FINALIZAÇÃO
**O que faz:** Exibe resumo final, grava timestamp de sucesso (se tudo deu certo) e salva os logs do processo

**Legenda dos blocos (explicação para leigos):**
- **Bloco 7.1:** Calcula quanto tempo levou todo o processo. **Como calcula?** Pega o horário atual (`fimExecucao = LocalDateTime.now()`) e subtrai do horário de início que foi registrado no Bloco 2.2 (`inicioExecucao`). Usa `Duration.between(inicio, fim).toMinutes()` para calcular a diferença em minutos. É como verificar o relógio no final do trabalho para saber quanto tempo levou.

- **Bloco 7.2:** Verifica se houve algum erro durante todo o processo. **Como verifica?** Checa se `totalFalhas == 0`. Se for zero, significa que todas as APIs (GraphQL, DataExport e Faturas GraphQL) foram executadas com sucesso. Se for maior que zero, significa que pelo menos uma API falhou. **O que conta?** O sistema conta quantas APIs deram certo (`totalSucessos`) e quantas deram errado (`totalFalhas`) durante toda a execução (Etapas 3A, 3B e 5).

- **Bloco 7.3:** Se tudo deu certo (`totalFalhas == 0`): mostra uma mensagem verde de sucesso. **O que faz?** Chama `BannerUtil.exibirBannerSucesso()` que exibe um banner colorido no terminal com emojis e formatação bonita. Depois exibe um resumo detalhado: quantas APIs foram executadas, quantos registros foram extraídos, quanto tempo levou, etc. **Importante:** Grava em um arquivo (`last_run.properties`) a data/hora de quando terminou com sucesso usando `gravarDataExecucao()`. **Por que grava?** Para saber quando foi a última execução bem-sucedida. Isso é útil para monitoramento e para saber se o sistema está rodando corretamente.

- **Bloco 7.4:** Se teve algum erro (`totalFalhas > 0`): mostra uma mensagem vermelha de erro. **O que faz?** Chama `BannerUtil.exibirBannerErro()` que exibe um banner vermelho no terminal indicando que houve problemas. Depois exibe um resumo com os problemas encontrados: quais APIs falharam, quantas falhas ocorreram, etc. **Importante:** NÃO grava a data/hora no arquivo `last_run.properties` porque não foi uma execução bem-sucedida. **Por que não grava?** Porque o arquivo `last_run.properties` é usado para rastrear execuções bem-sucedidas. Se gravasse mesmo com erro, não daria para saber se a última execução foi bem-sucedida ou não.

- **Bloco 7.5:** Termina o método `executar()` e volta para o programa principal (`Main.java`). **O que acontece?** O método `executar()` do `ExecutarFluxoCompletoComando` finaliza normalmente (retorna void, sem exceção). O controle volta para o `Main.java` que chamou esse método no Bloco 1.7. É como sair de uma função e voltar para onde estava antes.

- **Bloco 7.6:** Salva todos os logs em um arquivo e finaliza o sistema completamente. **O que faz?** O `Main.java` tem um bloco `finally` que sempre executa, mesmo se der erro. Esse bloco chama `loggingService.pararCaptura()` que salva todos os logs que foram capturados durante a execução em um arquivo de texto. **O que são logs?** São registros de tudo que aconteceu: mensagens de sucesso, erros, avisos, informações sobre quantos registros foram extraídos, etc. Depois de salvar os logs, o sistema finaliza completamente. É como fechar o diário e guardar na gaveta - tudo que aconteceu foi registrado e está salvo para consulta futura.

```
┌─────────────────────────────────────────────────────────────┐
│ [7.1] [comandos.extracao/ExecutarFluxoCompletoComando.java] │
│ [RESUMO FINAL]                                               │
│ fimExecucao = LocalDateTime.now()                           │
│ duracaoMinutos = Duration.between(inicio, fim).toMinutes()  │
│ Calcula duração total da execução                            │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [7.2] [DECISÃO] totalFalhas == 0?                            │
│ Verifica se houve falhas                                     │
└──────────┬───────────────────────────────┬──────────────────┘
           │ SIM                           │ NÃO
           │ (sem falhas)                  │ (com falhas)
           │                               │
           ▼                               ▼
┌──────────────────────┐      ┌──────────────────────────────┐
│ [7.3] BannerUtil.     │      │ [7.4] BannerUtil.            │
│   exibirBannerSucesso │      │   exibirBannerErro()          │
│                      │      │                               │
│ Log resumo sucesso   │      │ Log resumo com falhas         │
│                      │      │                               │
│ gravarDataExecucao() │      │ (NÃO grava timestamp)         │
│ └─ Salvar em         │      │                               │
│    last_run.properties│     │                               │
│ Se sucesso: exibe banner verde e grava timestamp            │
└──────────┬───────────┘      └──────────┬───────────────────┘
           │                              │
           │                              │
           └──────────────┬───────────────┘
                          │
                          │ ═══════════════════════════════════
                          │ CONVERGÊNCIA: Ambos caminhos chegam
                          │ aqui porque, independente de ter sido
                          │ sucesso (7.3 - grava timestamp) ou
                          │ falha (7.4 - não grava timestamp),
                          │ o método executar() precisa finalizar
                          │ normalmente e retornar ao Main.java
                          │ para executar o finally que salva logs.
                          │ ═══════════════════════════════════
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ [7.5] [RETORNO DO MÉTODO executar()]                        │
│ └─ Método finaliza normalmente                               │
│ Retorna do método executar()                                 │
│ (Ambos caminhos 7.3 e 7.4 convergem aqui)                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [7.6] [br.com.extrator/Main.java]                           │
│ [FINALLY] (sempre executado)                                 │
│ loggingService.pararCaptura()                                │
│ └─ Salvar logs em arquivo                                    │
│                                                              │
│ [FIM] Sistema finalizado                                     │
│ Salva logs em arquivo e finaliza sistema                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔑 DETALHAMENTO DOS COMPONENTES PRINCIPAIS

### Fluxo de Extração de uma Entidade (Exemplo: Coletas)

**⚠️ NOTA:** Esta seção detalha o fluxo completo de extração usando **Coletas como exemplo**. O mesmo processo se aplica a todas as outras entidades do sistema (Fretes, Manifestos, Cotações, Localização de Carga, Contas a Pagar, Faturas por Cliente, Faturas GraphQL). A única diferença entre as entidades é:
- O Extractor usado (ex: `ColetaExtractor`, `FreteExtractor`, `ManifestoExtractor`)
- O método da API chamado (ex: `buscarColetas()`, `buscarFretes()`, `buscarManifestos()`)
- O Mapper usado (ex: `ColetaMapper`, `FreteMapper`, `ManifestoMapper`)
- O Repository usado (ex: `ColetaRepository`, `FreteRepository`, `ManifestoRepository`)
- A tabela de destino no banco (ex: `coletas`, `fretes`, `manifestos`)

O processo de paginação, retry, transformação e persistência é idêntico para todas as entidades.

```
┌─────────────────────────────────────────────────────────────┐
│ [runners.graphql.extractors/ColetaExtractor.java]          │
│ extract(dataHoje, dataHoje)                                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [api/ClienteApiGraphQL.java]                                │
│ buscarColetas(dataHoje, dataHoje)                           │
│ └─ executarQueryPaginada(query, "coletas", variaveis,       │
│    ColetaNodeDTO.class)                                      │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [LOOP DE PAGINAÇÃO]                                         │
│ while (hasNextPage) {                                        │
│   ┌──────────────────────────────────────────────────────┐ │
│   │ [util.http/GerenciadorRequisicaoHttp.java]          │ │
│   │ getInstance() (Singleton)                            │ │
│   │ executarRequisicao(cliente, requisicao, "coletas")   │ │
│   │ ┌────────────────────────────────────────────────┐ │ │
│   │ │ [THROTTLING GLOBAL]                              │ │ │
│   │ │ lockThrottling.lock()                            │ │ │
│   │ │ [SE tempo < 2200ms] → Aguardar diferença         │ │ │
│   │ │ ultimaRequisicaoTimestamp = now                   │ │ │
│   │ │ lockThrottling.unlock()                           │ │ │
│   │ └────────────────────────────────────────────────┘ │ │
│   │                                                       │ │
│   │ [LOOP RETRY] for (tentativa = 1; tentativa <= 5)    │ │
│   │ ┌────────────────────────────────────────────────┐ │ │
│   │ │ cliente.send(requisicao)                        │ │ │
│   │ │ [DECISÃO] statusCode >= 200 && < 300?           │ │ │
│   │ │ ├─ SIM → Retornar resposta                       │ │ │
│   │ │ └─ NÃO → [DECISÃO] deveRetentar(statusCode)?   │ │ │
│   │ │    ├─ NÃO → Retornar resposta (erro definitivo)  │ │ │
│   │ │    └─ SIM → [DECISÃO] statusCode == 429?       │ │ │
│   │ │       ├─ SIM → Aguardar 2s, continuar loop    │ │ │
│   │ │       └─ NÃO → [DECISÃO] statusCode >= 500?    │ │ │
│   │ │          ├─ SIM → Backoff exponencial           │ │ │
│   │ │          └─ NÃO → Continuar                     │ │ │
│   │ └────────────────────────────────────────────────┘ │ │
│   └──────────────────────────────────────────────────────┘ │
│   │                                                         │
│   │ [DESERIALIZAÇÃO]                                        │
│   │ ObjectMapper.readValue() → List<ColetaNodeDTO>         │
│   │                                                         │
│   │ [ATUALIZAÇÃO]                                           │
│   │ cursor = resposta.getEndCursor()                       │
│   │ hasNextPage = resposta.hasNextPage                    │
│   └──────────────────────────────────────────────────────┘ │
│ }                                                            │
│                                                              │
│ [RETORNO] ResultadoExtracao.completo(todasColetas)          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [TRANSFORMAÇÃO]                                             │
│ [modelo.graphql.coletas/ColetaMapper.java]                 │
│ toEntity(ColetaNodeDTO dto)                                 │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ [DECISÃO] dto == null?                                │   │
│ │ └─ SIM → Retornar null                                 │   │
│ │                                                         │   │
│ │ [MAPEAMENTO]                                           │   │
│ │ entity.setId(dto.getId())                              │   │
│ │ entity.setSequenceCode(dto.getSequenceCode())         │   │
│ │                                                         │   │
│ │ [CONVERSÃO DE TIPOS]                                   │   │
│ │ [SE String data] → LocalDate.parse()                   │   │
│ │ [SE String valor] → BigDecimal.parse()                 │   │
│ │                                                         │   │
│ │ [TRUNCAMENTO]                                          │   │
│ │ [SE string.length() > max] → Truncar                   │   │
│ │                                                         │   │
│ │ [SERIALIZAÇÃO METADATA]                                │   │
│ │ ObjectMapper.writeValueAsString(dto)                   │   │
│ │ └─ entity.setMetadata(json)                            │   │
│ └──────────────────────────────────────────────────────┘   │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [PERSISTÊNCIA]                                              │
│ [db.repository/ColetaRepository.java]                       │
│ salvar(List<ColetaEntity> entities)                         │
│ └─ [HERDA DE] AbstractRepository.salvar()                   │
│    ┌──────────────────────────────────────────────────┐  │
│    │ [DECISÃO] entities == null || vazia?                │  │
│    │ └─ SIM → Retornar 0                                 │  │
│    │                                                      │  │
│    │ [ABERTURA]                                          │  │
│    │ Connection conexao = obterConexao()                 │  │
│    │ conexao.setAutoCommit(false)                        │  │
│    │                                                      │  │
│    │ [VALIDAÇÃO]                                         │  │
│    │ verificarTabelaExisteOuLancarErro()                 │  │
│    │ └─ [SE não existe] → Lançar SQLException            │  │
│    │                                                      │  │
│    │ [LOOP] Para cada entidade:                          │  │
│    │ ┌────────────────────────────────────────────────┐ │  │
│    │ │ [EXECUÇÃO MERGE]                                 │ │  │
│    │ │ executarMerge(conexao, entidade)                 │ │  │
│    │ │ └─ Construir SQL MERGE                           │ │  │
│    │ │    MERGE INTO coletas AS target                  │ │  │
│    │ │    USING (VALUES ...) AS source                  │ │  │
│    │ │    ON target.id = source.id                      │ │  │
│    │ │    WHEN MATCHED THEN UPDATE ...                  │ │  │
│    │ │    WHEN NOT MATCHED THEN INSERT ...              │ │  │
│    │ │                                                      │ │  │
│    │ │ [DECISÃO] rowsAffected > 0?                       │ │  │
│    │ │ ├─ SIM → totalSucesso++                           │ │  │
│    │ │ └─ NÃO → Log warning                              │ │  │
│    │ │                                                      │ │  │
│    │ │ [DECISÃO] SQLException?                           │ │  │
│    │ │ ├─ SIM → totalFalhas++                           │ │  │
│    │ │ │       └─ [DECISÃO] continuarAposErro?          │ │  │
│    │ │ │          ├─ SIM → Continuar                    │ │  │
│    │ │ │          └─ NÃO → Rollback, lançar exceção     │ │  │
│    │ │ └─ NÃO → Continuar                               │ │  │
│    │ │                                                      │ │  │
│    │ │ [COMMIT BATCH]                                     │ │  │
│    │ │ [SE registroAtual % batchSize == 0]               │ │  │
│    │ │ └─ conexao.commit()                                │ │  │
│    │ └────────────────────────────────────────────────┘ │  │
│    │                                                      │  │
│    │ [COMMIT FINAL]                                       │  │
│    │ conexao.commit()                                     │  │
│    │                                                      │  │
│    │ [RETORNO] totalSucesso                              │  │
│    └──────────────────────────────────────────────────┘  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ [REGISTRO DE LOG]                                           │
│ [db.repository/LogExtracaoRepository.java]                 │
│ gravarLogExtracao(LogExtracaoEntity)                        │
│ └─ Salvar estatísticas da extração no banco                 │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 RESUMO DO FLUXO

### Entidades Extraídas (Últimas 24h - Dia Atual)

**API GraphQL (Thread 1):**
1. Usuários do Sistema (dependência de Coletas)
2. Coletas
3. Fretes
4. Faturas GraphQL (Fase 3 - sequencial)

**API Data Export (Thread 2):**
1. Manifestos
2. Cotações
3. Localização de Carga
4. Contas a Pagar
5. Faturas por Cliente

### Características do Fluxo

- **Execução Paralela**: 2 threads simultâneas (GraphQL + DataExport)
- **Throttling Global**: 2 segundos mínimo entre requisições (compartilhado)
- **Retry**: Até 5 tentativas com backoff exponencial
- **Circuit Breaker**: 5 falhas consecutivas → desabilitar entidade
- **Persistência**: MERGE (UPSERT) com batch commits
- **Validação**: Completude, gaps e janela temporal

---

**Última Atualização:** 23/01/2026  
**Versão do Sistema:** 2.3.1  
**Status:** ✅ Pronto para uso no Miro
