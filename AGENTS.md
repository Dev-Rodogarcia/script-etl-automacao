# Regras Para IAs Neste Projeto

Este projeto e o dono das extracoes, tabelas, views, indices e migrations do ETL.

- Cada macaco no seu galho: mudancas de schema, regra materializada ou view do ETL ficam neste repositorio. Nao coloque migrations do ETL no projeto `dashboards`.
- O projeto `dashboards` deve consumir o contrato publicado pelo ETL; se o contrato precisar mudar, altere primeiro aqui e deixe a mudanca explicita em migration, script base e view.
- Sempre que criar ou alterar uma migration em `database/migrations`, atualize tambem os scripts canonicos correspondentes em `database/tabelas`, `database/views`, `database/indices` e `database/validacao`, quando aplicavel. Uma recriacao limpa do banco precisa nascer ja atualizada.
- Regras caras de BI que dependem de normalizacao, data de referencia ou elegibilidade devem ser materializadas nas tabelas durante a carga/backfill do ETL sempre que forem usadas por dashboards recorrentes.
- Encoding e mojibake: preserve acentos, simbolos e aliases exatamente como publicados pelo banco/API. Nao aceite texto corrompido por encoding, como UTF-8 lido como ANSI/Windows-1252; se encontrar alias, migration, view, CSV, seed, fixture, doc ou teste com caracteres estranhos no lugar de acentos/simbolos, corrija a origem e valide usando UTF-8 antes de seguir.
- Nao execute rotinas de producao nem scripts de start de producao sem pedido explicito do operador humano.
