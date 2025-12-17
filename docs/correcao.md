Com base na análise profunda do arquivo `Model.bim` fornecido, preparei uma documentação técnica detalhada. O objetivo é permitir que você replique a lógica de negócios, transformações (ETL) e cálculos (KPIs) no seu ambiente Python (Pandas/Polars/Dash), sem a necessidade de classes, focando na lógica funcional.

O modelo contém **7 Tabelas Fato principais** (a 8ª, Ocorrências, não consta neste arquivo JSON) e tabelas dimensão de suporte. A lógica está dividida entre Power Query (M) para limpeza e DAX para métricas.

---

#Documentação Técnica de Migração: Power BI para Python##1. Visão Geral do Modelo* **Estrutura:** Star Schema (Esquema Estrela).
* **Tabelas Fato:** `fFretes`, `fColetas`, `fManifestos`, `fContasAPagar`, `fCotacoes`, `fFaturasCliente`, `fLocalizacaoCargas`.
* **Tabelas Dimensão:** `dCalendario`, `dFiliais`, `dClientes`, `dMotoristas`, `dVeiculos`, `dPlanoContas`.
* **Tabela de Medidas:** `_Medidas` (Centraliza os cálculos KPI).

---

##2. Dimensões Globais (Tratamento Prévio)Antes de processar os fatos, você precisará replicar as dimensões para garantir filtros corretos.

###dCalendario* **Lógica Power BI:** Criada dinamicamente buscando a menor data entre `fFretes` e `fManifestos` até o final do ano atual.
* **Python:** Crie um DataFrame de datas (`pd.date_range`) cobrindo `min(df_fretes['Data frete'])` até `today`.
* **Colunas Necessárias:** Data, Ano, Mês (Número e Nome), Trimestre, Dia da Semana, Flag `IsFimDeSemana` (Sábado/Domingo).

###dFiliais* **Fonte:** Derivada da tabela `fColetas`.
* **Lógica:** `SELECT DISTINCT Filial, [Filial ID] FROM fColetas`.
* **Uso:** Usada para JOIN em todas as tabelas fato via `ID_Filial`.

---

##3. Entidades (Tabelas Fato) e KPIsAbaixo, a lógica detalhada por entidade.

###3.1. Entidade: Fretes (`fFretes`)Focada na receita e volume de transporte.

**A. ETL e Transformações (Power Query -> Python)**

1. **Fonte:** `vw_fretes_powerbi`.
2. **Coluna Calculada `Status_Nome`:**
* Mapear `Status` (inglês) para PT-BR:
* `finished` -> "Concluído"
* `pending` -> "Pendente"
* `delivering` -> "Em Entrega"
* `cancelled` -> "Cancelado"
* `standby` -> "Espera"
* `in_transit` -> "Em Trânsito"
* `manifested` -> "Manifestado"
* `occurrence_treatment` -> "Tratamento de ocorrência"




3. **Coluna Calculada `Rota`:** Concatenar: `[Origem] + " (" + [UF Origem] + ") → " + [Destino] + " (" + [UF Destino] + ")"`.
4. **Coluna Calculada `Modal_PT`:** Traduzir `rodo` -> "Rodoviário", `aereo` -> "Aéreo", `maritimo` -> "Marítimo".

**B. Métricas e KPIs (DAX -> Python)**

* **`FRE_Fretes Qtde`:** Contagem simples de linhas (`count`) ou IDs únicos.
* **`FRE_Valor Total`:** Soma da coluna `Valor Total do Serviço`.
* **`FRE_Peso Taxado`:** Soma da coluna `Kg Taxado`.
* **`FRE_Ticket Medio`:** `FRE_Valor Total` / `FRE_Fretes Qtde`.
* **`FRE_Valor por Kg`:** `FRE_Valor Total` / `FRE_Peso Taxado`.
* **`FRE_Var Valor YOY` (Year Over Year):**
* *Python:* Calcular a soma de `Valor Total do Serviço` agrupado por ano. Calcular `(Valor Ano Atual - Valor Ano Anterior) / Valor Ano Anterior`.
* *Nota:* O modelo trata divisões por zero retornando `BLANK` (no Python use `np.nan` ou trate com `fillna(0)`).


* **`FRE_Top5 Rotas`:** Agrupar por `Rota`, somar `Valor Total do Serviço`, ordenar DESC e pegar top 5.

---

###3.2. Entidade: Coletas (`fColetas`)Focada na eficiência da primeira milha.

**A. ETL e Transformações**

1. **Fonte:** `vw_coletas_powerbi`.
2. **Coluna `Status_PT`:** Tradução similar a fretes (`finished` -> "Finalizada", etc).
3. **Coluna `No_Prazo`:** Lógica condicional:
* Se `Finalizacao` <= `Agendamento`: "Sim"
* Senão: "Não" (Considerar nulos como "Não" ou tratar à parte).



**B. Métricas e KPIs**

* **`COL_Coletas Qtde`:** Contagem de linhas (`id`).
* **`COL_Taxa Conclusao`:**
* Numerador: Contagem onde `Status` == "finalizado".
* Denominador: `COL_Coletas Qtde` Total.


* **`COL_Tempo Medio Horas`:**
* Cálculo: Média da diferença em horas entre `Finalizacao` e `Solicitacao`.
* *Python:* `(df['Finalizacao'] - df['Solicitacao']).dt.total_seconds() / 3600`.


* **`COL_Cor Pontualidade` (Regra de Negócio para Dashboard):**
* Verde: Taxa >= 95%
* Amarelo: Taxa >= 85%
* Vermelho: < 85%



---

###3.3. Entidade: Manifestos (`fManifestos`)Focada em custos e viagens (MDF-e).

**A. ETL e Transformações**

1. **Fonte:** `vw_manifestos_powerbi`.
2. **Tratamento Booleano:** Converter colunas `Km manual`, `Gerar MDF-e`, `Solicitou Monitoramento` de texto ("Sim"/"Não" ou "1"/"0") para Booleano (`True`/`False`).

**B. Métricas e KPIs**

* **`MAN_Manifestos Qtde`:** Contagem de linhas.
* **`MAN_Custo Total`:** Soma da coluna `Custo total`.
* **`MAN_Valor Fretes`:** Soma da coluna `Fretes/Total` (Receita atrelada ao manifesto).
* **`MAN_Rentabilidade`:**
* Formula: `( [MAN_Valor Fretes] - [MAN_Custo Total] ) / [MAN_Valor Fretes]`.
* *Meta:* >= 20% (Verde), >= 10% (Amarelo), < 10% (Vermelho).


* **`MAN_Custo por KM`:** `[MAN_Custo Total]` / Soma de `KM viagem`.
* **`KPI_Utilizacao Frota`:**
* Numerador: Soma de `Total peso taxado`.
* Denominador: Soma de `Carreta 1/Capacidade Peso`.



---

###3.4. Entidade: Financeiro / Contas a Pagar (`fContasAPagar`)Focada em despesas e fluxo de caixa passivo.

**A. ETL e Transformações**

1. **Fonte:** `vw_contas_a_pagar_powerbi`.
2. **Coluna Crítica `Flag_Operacional`:**
* Regra: Se `Centro de custo/Nome` for "ADMINISTRATIVO GERAL" **OU** `Fornecedor/Nome` for "RODOGARCIA TRANSPORTES RODOVIARIOS LTDA", então `0`, senão `1`.
* *Importante:* Isso define o que é Custo Operacional vs Despesa Administrativa.


3. **Coluna `Vencimento Estimado`:** Se `Emissão` for nulo, usar `Data criação`. Somar 30 dias a essa data base.

**B. Métricas e KPIs**

* **`CAP_Valor A Pagar Operacional`:** Soma de `Valor a pagar` filtrando onde `Flag_Operacional == 1`.
* **`CAP_Valor Pago`:** Soma de `Valor pago` (considerando a data de liquidação para filtro temporal, `USERELATIONSHIP` no Power BI).
* **`CAP_Vencidas Qtde`:** Contagem onde `Status Pagamento` == "ABERTO" e `Vencimento Estimado` < Data de Hoje.

---

###3.5. Entidade: Comercial / Cotações (`fCotacoes`)Funil de vendas.

**A. ETL e Transformações**

1. **Fonte:** `vw_cotacoes_powerbi`.
2. **Coluna `Status_Cotacao`:**
* Lógica: Se a coluna `CT-e/Data de emissão` não for nula, então "CONVERTIDA", senão "PENDENTE".



**B. Métricas e KPIs**

* **`COT_Realizadas Qtde`:** Contagem de `N° Cotação`.
* **`COT_Convertidas Qtde`:** Contagem onde `Status Conversão` (ou a calculada `Status_Cotacao`) == "CONVERTIDA".
* **`COT_Taxa Conversao`:** `Convertidas` / `Realizadas`.
* **`COT_Rank Cliente Valor`:** Rankeamento denso dos clientes baseado na soma de `Valor frete`.

---

###3.6. Entidade: Faturas (`fFaturasCliente`)Contas a Receber.

**A. ETL e Transformações**

1. **Fonte:** `vw_faturas_por_cliente_powerbi`.
2. **Filtro:** Apenas linhas onde `CT-e/Data de emissão` não é nulo.

**B. Métricas e KPIs**

* **`FAT_Valor Fatura`:** Soma de `Fatura/Valor`.
* **`FAT_Valor Aberto`:** Soma de `Fatura/Valor` onde `Fatura/Baixa` (data de pagamento) é Nulo (BLANK).
* **`FAT_CTes Cancelados`:** Contagem onde `CT-e/Status` == "Cancelado".

---

###3.7. Entidade: Localização (`fLocalizacaoCargas`)Rastreamento.

**A. ETL e Transformações**

1. **Fonte:** `vw_localizacao_cargas_powerbi`.

**B. Métricas e KPIs**

* **`LOC_Total Cargas`:** Contagem de linhas.
* **`LOC_Em Transito`:** Contagem onde `Status Carga` == "in_transfer".
* **`LOC_Principal Destino`:** A região (`Região Destino`) com maior volume de cargas (Top 1).

---

##4. KPIs Executivos (Cálculo Cruzado)Estes são os cálculos que cruzam tabelas e devem ser feitos com cuidado no Python (geralmente requerem `merge` ou agregação prévia por período).

1. **`KPI_Receita Total`:**
* `Soma(fFretes[Valor Total])` + `Soma(fFaturasCliente[Valor Fatura])`.


2. **`KPI_Custo Total`:**
* `Soma(fManifestos[Custo Total])` + `Soma(fContasAPagar[Valor a pagar])` (Apenas Operacional: Flag=1).


3. **`KPI_Lucro Bruto`:**
* `Receita Total` - `Custo Total`.


4. **`KPI_Margem Bruta`:**
* `Lucro Bruto` / `Receita Total`.


5. **`RES_Resultado Liquido` (Visão de Caixa):**
* `Receita (Faturas)` - (`Custos Viagem (Manifestos)` + `Despesas Fixas (ContasAPagar Original)`).



---

##5. Dicas para Implementação no Python1. **Datas (`USERELATIONSHIP`):**
* No Power BI, medidas como `CAP_Valor Pago` usam a "Data de Liquidação" em vez da "Data de Emissão".
* **No Python:** Ao calcular fluxo de caixa realizado, não use a coluna de data padrão do índice. Você deve fazer um `groupby` pela coluna `Baixa/Data liquidação` para somar os valores pagos naquele período.


2. **Filtros de Contexto:**
* Medidas como `COT_Var Qtde YOY` exigem que você desloque os dados (shift). Use `df.groupby('Ano')['Valor'].sum().pct_change()` para facilitar.


3. **Relacionamentos:**
* Como não há motor de relação automática no Pandas, faça *merges* (left join) das tabelas fato com a tabela `dFiliais` usando `ID_Filial` antes de agrupar, caso precise quebrar os KPIs por Filial.


4. **Ocorrências:**
* Como a tabela não existe no JSON, você precisará importar a view `vw_ocorrencias` do seu banco SQL e criar a lógica padrão: Contagem de Ocorrências, % de Resolução (se houver status) e Tempo Médio de Tratamento.



Este guia cobre toda a lógica de negócio contida no arquivo `.bim`. Você pode agora traduzir cada item de "Métrica" para uma linha de código Pandas/Polars.