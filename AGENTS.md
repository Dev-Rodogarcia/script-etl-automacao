# ⚙️ Regras Operacionais para IAs - CLI ETL Daemon

Você atua como Engenheiro de Software Principal neste repositório (Java 17 CLI Extrator e Scripts SQL). Seu objetivo é garantir a integridade da extração, transformação e governança estrutural do banco analítico.

---

## 🟢 Permissão de Escrita e Preparação de Banco

* **Owner Estrutural:** Este repositório é o único dono do banco/schema `ETL_SISTEMA` (`esl_cloud`). Você tem permissão total para criar e alterar tabelas base, migrations, índices, views operacionais (`dbo.vw_*_powerbi`) e views dimensionais.
* **Preservação de Documentação Operacional:** É proibido apagar arquivos `README.md` e `AGENTS.md`. Quando necessário, apenas atualize seu conteúdo mantendo esses arquivos presentes no repositório.
* **Preparação para o Dashboard:** Garanta que todas as alterações de infraestrutura de dados necessárias para o Dashboard estejam aplicadas, testadas e documentadas aqui, deixando o caminho livre para o consumo limpa pelo monorepo de painéis.
* **Sincronismo Canônico:** Ao criar ou alterar uma migration em `database/migrations`, atualize obrigatoriamente os scripts base correspondentes em `database/tabelas`, `database/views`, `database/indices` ou `database/validacao`. O banco deve ser capaz de ser recriado do zero de forma limpa.

---

## 🗄️ Topologia de Bancos de Dados e Fronteiras Arquiteturais

* **`ETL_SISTEMA` (`esl_cloud`):** Trate este banco como domínio exclusivo do pipeline de extração e como fonte de verdade analítica. Este repositório é o único local autorizado para aplicar DDL/DML estrutural neste banco. Crie colunas computadas, chaves, índices, tabelas base, views operacionais (`dbo.vw_*_powerbi`) e views analíticas exclusivamente via `database/migrations` e mantenha os scripts base sincronizados. O backend do Dashboard deve consumir este banco estritamente em modo **READ-ONLY** (`SELECT`).
* **`DASHBOARDS`:** Trate este banco como produção exclusiva da aplicação web. Ele armazena somente estado interno do portal: ACL (papéis, permissões, usuários e setores), sessões, configurações e auditoria administrativa. Não aplique DDL/DML deste repositório no banco `DASHBOARDS`. A única fonte de verdade estrutural dele é o **Flyway** do monorepo Dashboard em `backend/src/main/resources/db/migration`. O Hibernate do portal deve operar com `ddl-auto=none`; DDL em runtime pelo Java é terminantemente proibido.
* **`DASHBOARDS_DEV`:** Trate este banco como sandbox de desenvolvimento local do portal, usado pelo profile `dev` e por `.env.development.local`. Ele existe para evitar acidentes e poluição de dados na produção. Preserve o contrato do validador `DevDatabaseIsolationValidator`, que executa *fast-fail* e aborta o startup do backend do Dashboard quando o ambiente de desenvolvimento tenta conectar a JDBC principal ao banco `DASHBOARDS` de produção.

---

## 🧠 Diretrizes de Performance e Dados (Data Quality)

* **Materialização Obrigatória:** Regras de BI complexas, filtros de elegibilidade pesados ou cruzamentos textuais não devem ser processados sob demanda dentro das views de apresentação. Realize o processamento textual pesado e as validações durante a carga (Load) no Java e salve o resultado em colunas físicas (ex: `BIT`, `TINYINT`) indexadas nas tabelas base.
* **Exclusão Lógica (Soft Delete):** Dados extraídos das APIs e cadastros de suporte não podem sofrer `TRUNCATE` ou `DELETE` físico em rotinas comuns. Use controle de vigência ou inativação para preservar históricos e auditorias de BI.

## Modelo Aditivo com Expurgo Logico (Sweep and Prune)

O ETL opera em modelo aditivo por padrao: o loop recorrente de 30 minutos deve executar apenas insercoes e atualizacoes de registros novos ou alterados. Ele nao deve fazer `DELETE`, `TRUNCATE` ou varredura completa de ausencia na origem, para nao acoplar reconciliacao historica ao caminho critico de extracao quase em tempo real.

Quando uma entidade da API deixar de retornar uma chave que ja existe no `ETL_SISTEMA`, a sincronizacao de estado deve ser feita por expurgo logico noturno. O job dedicado de reconciliacao deve obter o snapshot de chaves da origem, comparar com as chaves ativas do banco e marcar os ausentes com `excluido_na_origem = 1`, preservando os dados para auditoria. Quando uma chave reaparecer na origem, o upsert deve reativar o registro com `excluido_na_origem = 0`.

Toda tabela de dominio sincronizada com APIs externas deve expor uma coluna padrao de controle, preferencialmente `excluido_na_origem BIT NOT NULL DEFAULT (0)`, acompanhada de metadados de auditoria quando aplicavel, como `data_exclusao_origem` e `ultima_reconciliacao_origem_em`. Alteracoes estruturais devem ser feitas por migrations e refletidas nos scripts base correspondentes.

As views operacionais, views analiticas, APIs e dashboards devem filtrar registros com `excluido_na_origem = 1` por padrao. Consultas que incluam excluidos logicos so sao permitidas para auditoria, diagnostico ou reconciliacao tecnica e devem declarar essa intencao explicitamente.

O job `Sweep and Prune` deve rodar fora do horario de pico, com paginacao por origem, staging ou comparacao por conjuntos de chaves, updates em lote, telemetria por entidade e protecao contra execucao concorrente. Se o snapshot de uma entidade falhar ou ficar incompleto, essa entidade nao deve ser marcada como expurgada naquela execucao.

* **Testes e Sanidade:** Antes de dar a tarefa por concluída, execute a suíte de testes locais (`src/test`) e os scripts de validação de schema (`database/validacao`). Nenhuma alteração estrutural pode subir sem validação de quebra de contrato.
* **Encoding e Mojibake:** Todo o ecossistema (código Java, drivers JDBC, scripts SQL e arquivos de log) opera estritamente em UTF-8. Não aceite aliases ou dados de tabelas com caracteres corrompidos.
