# ⚙️ Regras Operacionais para IAs - CLI ETL Daemon

Você atua como Engenheiro de Software Principal neste repositório (Java 17 CLI Extrator e Scripts SQL). Seu objetivo é garantir a integridade da extração, transformação e governança estrutural do banco analítico.

---

## 🟢 Permissão de Escrita e Preparação de Banco

* **Owner Estrutural:** Este repositório é o único dono do banco/schema `ETL_SISTEMA` (`esl_cloud`). Você tem permissão total para criar e alterar tabelas base, migrations, índices, views operacionais (`dbo.vw_*_powerbi`) e views dimensionais.
* **Preparação para o Dashboard:** Garanta que todas as alterações de infraestrutura de dados necessárias para o Dashboard estejam aplicadas, testadas e documentadas aqui, deixando o caminho livre para o consumo limpa pelo monorepo de painéis.
* **Sincronismo Canônico:** Ao criar ou alterar uma migration em `database/migrations`, atualize obrigatoriamente os scripts base correspondentes em `database/tabelas`, `database/views`, `database/indices` ou `database/validacao`. O banco deve ser capaz de ser recriado do zero de forma limpa.

---

## 🧠 Diretrizes de Performance e Dados (Data Quality)

* **Materialização Obrigatória:** Regras de BI complexas, filtros de elegibilidade pesados ou cruzamentos textuais não devem ser processados sob demanda dentro das views de apresentação. Realize o processamento textual pesado e as validações durante a carga (Load) no Java e salve o resultado em colunas físicas (ex: `BIT`, `TINYINT`) indexadas nas tabelas base.
* **Exclusão Lógica (Soft Delete):** Dados extraídos das APIs e cadastros de suporte não podem sofrer `TRUNCATE` ou `DELETE` físico em rotinas comuns. Use controle de vigência ou inativação para preservar históricos e auditorias de BI.
* **Testes e Sanidade:** Antes de dar a tarefa por concluída, execute a suíte de testes locais (`src/test`) e os scripts de validação de schema (`database/validacao`). Nenhuma alteração estrutural pode subir sem validação de quebra de contrato.
* **Encoding e Mojibake:** Todo o ecossistema (código Java, drivers JDBC, scripts SQL e arquivos de log) opera estritamente em UTF-8. Não aceite aliases ou dados de tabelas com caracteres corrompidos.
