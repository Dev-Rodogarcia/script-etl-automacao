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
Nome Formal da Arquitetura: Processo de ETL (Extract, Transform, Load) com Camada de Persistência Híbrida e Desacoplada
O que estamos construindo é um sistema de ETL. Este é o termo técnico padrão da indústria para o processo que estamos implementando:

Extract (Extrair): É o ato de buscar os dados brutos da fonte, no nosso caso, a API da ESL Cloud.

Transform (Transformar): É a etapa mais crítica. É o processo de limpar, organizar, validar e modelar os dados extraídos para que fiquem no formato que o nosso sistema de destino (o banco de dados SQL Server) precisa. As classes DTO, Mapper e Entity são o coração desta fase.

Load (Carregar): É a etapa final, onde os dados já transformados são carregados no sistema de destino. A classe Repository é a responsável por esta fase.

A nossa abordagem é "Híbrida" porque combinamos o melhor de dois mundos: a estrutura rígida de um banco de dados relacional para os campos-chave e a flexibilidade de um formato de documento (JSON) para os dados detalhados. E é "Desacoplada" porque cada etapa do processo é independente; uma mudança na API (Extract) não quebra a lógica de carregamento no banco de dados (Load).

Os Princípios Fundamentais Desta Arquitetura
Antes de detalhar as classes, é crucial entender os pilares que sustentam esta decisão. Esta arquitetura foi escolhida para garantir:

Resiliência Absoluta a Mudanças: O objetivo principal é que o sistema não quebre quando a API da ESL Cloud adicionar, remover ou alterar campos não essenciais. O sistema deve se adaptar, não falhar.

Completude Total dos Dados: Garantir que 100% dos dados fornecidos pela API sejam capturados e armazenados, sem exceção. Nada pode ser perdido no processo.

Manutenibilidade e Clareza: O código deve ser fácil de entender, manter e evoluir. Qualquer desenvolvedor (ou IA) deve ser capaz de identificar rapidamente a responsabilidade de cada componente, evitando a criação de classes "faz-tudo" que são frágeis e difíceis de depurar.

Isolamento de Complexidade: Cada entidade (Cotações, Manifestos, etc.) é tratada como um universo isolado. Uma mudança na lógica de Faturas a Receber nunca, jamais, deve impactar o funcionamento de Coletas.

O Blueprint Detalhado: A Linha de Montagem de Dados
Para cada uma das 8 entidades, construiremos uma "linha de montagem" especializada, composta por quatro estações (as quatro classes). Cada estação tem uma função única e bem definida.

Estação 1: O DTO (Data Transfer Object)
Apelido: O Receptor Universal.

Objetivo Principal: Servir como o ponto de entrada, capturando uma cópia fiel e completa dos dados brutos (JSON) vindos da API, de forma flexível.

Responsabilidades Detalhadas:

Declaração de Campos Essenciais: Esta classe terá declarada, de forma explícita, uma quantidade mínima de atributos. Estes são os campos que usaremos para identificar unicamente um registro (a "chave de negócio"). Por exemplo, para um Manifesto, seriam os campos id, manifest_number e service_date.

Criação do Contêiner Dinâmico: O componente mais importante desta classe é um mapa (ex: Map<String, Object>). Este mapa servirá como um "contêiner resto", projetado para capturar automaticamente todos os outros campos do JSON que não foram declarados explicitamente. Isso garante que, se a API enviar um campo novo amanhã, ele não será ignorado, mas sim armazenado neste contêiner.

Mapeamento API -> Objeto: A classe utilizará anotações (como @JsonProperty e @JsonAnySetter de bibliotecas como a Jackson) para instruir o sistema sobre como converter o JSON da API neste objeto Java.

O que esta classe NÃO FAZ:

Ela não contém lógica de negócio.

Ela não sabe o que é um banco de dados.

Ela não converte tipos de dados (ex: de um texto "2025-10-26" para um objeto de data). Ela recebe tudo da forma mais bruta possível para evitar erros na desserialização.

Estação 2: O Mapper
Apelido: O Controlador de Qualidade e Tradutor.

Objetivo Principal: Transformar o DTO (dados brutos e flexíveis) em uma Entidade (dados estruturados e prontos para o banco de dados).

Responsabilidades Detalhadas:

Tradução e Conversão: O Mapper recebe o DTO da Estação 1. Ele extrai os campos essenciais e os converte para os tipos de dados corretos que o banco de dados espera (ex: converte a String de data em um LocalDate). É aqui que a "limpeza" dos dados acontece.

Tratamento de Erros: Se um dado essencial vier em um formato inválido (ex: uma data malformada), é responsabilidade do Mapper lidar com isso, seja registrando um erro ou definindo um valor padrão, garantindo que o sistema não quebre.

Empacotamento dos Metadados: O Mapper pega o "Contêiner Resto" do DTO, unifica-o com os campos essenciais (para garantir que a informação não se perca) e serializa tudo isso em uma única String no formato JSON. Esta String representa a totalidade dos dados daquele registro.

Construção da Entidade: Ao final, o Mapper cria e preenche um objeto da classe Entity (Estação 3), que está pronto para ser enviado para a próxima etapa.

O que esta classe NÃO FAZ:

Ela não se comunica com a API.

Ela não se comunica com o banco de dados. Sua única função é a transformação de um tipo de objeto em outro.

Estação 3: A Entity (Entidade)
Apelido: O Produto Final Padronizado.

Objetivo Principal: Representar de forma exata e limpa a estrutura de uma linha na tabela do banco de dados SQL Server.

Responsabilidades Detalhadas:

Estrutura Espelhada: Os atributos desta classe são um espelho 1-para-1 das colunas da tabela correspondente no banco. Para um Manifesto, teria os atributos id, manifestNumber, serviceDate e metadata.

Tipagem Forte: Os tipos de dados dos atributos correspondem exatamente aos tipos de dados das colunas no SQL Server (String, LocalDate, etc.).

Pureza de Dados: Esta classe é um "POJO" (Plain Old Java Object). Ela contém apenas os dados e os métodos para acessá-los (getters e setters).

O que esta classe NÃO FAZ:

Ela não contém lógica de negócio complexa.

Ela não sabe como foi criada (não conhece o Mapper nem o DTO).

Ela não sabe como será salva (não conhece o Repository).

Estação 4: O Repository (Repositório)
Apelido: O Operador de Logística.

Objetivo Principal: Ser a única e exclusiva camada de comunicação com o banco de dados para uma entidade específica, implementando a lógica de persistência (carregamento).

Responsabilidades Detalhadas:

Abstração do Acesso a Dados: O Repository esconde toda a complexidade da comunicação com o banco de dados (abrir conexão, preparar comandos SQL, etc.).

Implementação da Lógica de MERGE (UPSERT): Esta é sua função mais importante. Ele recebe o objeto Entity da Estação 3 e constrói a instrução MERGE do SQL Server. A cláusula ON desta instrução usará os campos-chave da Entity para determinar se um registro já existe.

Execução das Operações: Com base na verificação do MERGE, ele executa uma das duas ações:

INSERT (Inserir): Se o registro for novo, ele insere uma nova linha na tabela.

UPDATE (Atualizar): Se o registro já existir, ele atualiza as colunas da linha existente (principalmente a coluna metadata com os dados mais recentes).

Gerenciamento de Transações: Garante que a operação de escrita no banco seja atômica (ou tudo funciona, ou nada é alterado), prevenindo a corrupção de dados.

O que esta classe NÃO FAZ:

Ela não transforma dados (isso é trabalho do Mapper).

Ela não conhece a API (não sabe de onde os dados vieram).

Ela não contém lógica de negócio; apenas lógica de persistência.