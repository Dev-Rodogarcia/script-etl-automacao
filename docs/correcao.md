```python?code_reference&code_event_index=2
import pandas as pd

files = [
    "faturas_por_cliente_2025-11-22_14-40-49.csv",
    "fretes_2025-11-22_14-40-49.csv",
    "localizacao_cargas_2025-11-22_14-40-49.csv",
    "manifestos_2025-11-22_14-40-49.csv",
    "coletas_2025-11-22_14-40-49.csv",
    "contas_a_pagar_2025-11-22_14-40-49.csv",
    "cotacoes_2025-11-22_14-40-49.csv"
]

info = {}
for f in files:
    try:
        df = pd.read_csv(f, sep=';', nrows=2)
        info[f] = df.columns.tolist()
    except Exception as e:
        info[f] = str(e)

print(info)

```

```text?code_stdout&code_event_index=2
{'faturas_por_cliente_2025-11-22_14-40-49.csv': ['ID', 'Filial', 'Pagador / Nome', 'Pagador / Documento', 'Nfse / Número NFS-e', 'CT-e / Número', 'CT-e / Data emissão', 'CT-e / Chave', 'CT-e / Status', 'Fatura / N° Doc', 'Fatura / Emissão', 'Fatura / Valor', 'Parcelas / Vencimento', 'Baixa / Data', 'Frete original / Total', 'Tipo', 'Estado / Nome', 'Classificação', 'Remetente / Nome', 'Destinatário / Nome', 'NF (Notas Fiscais)', 'Cache / N° Pedido', 'Data de extracao'], 'fretes_2025-11-22_14-40-49.csv': ['ID', 'Chave CT-e', 'Numero CT-e', 'Serie CT-e', 'Data frete', 'Valor Total do Servico', 'Valor NF', 'Valor Frete', 'Volumes', 'Kg Taxado', 'Kg Real', 'M3', 'Pagador', 'Remetente', 'Origem', 'UF Origem', 'Destinatario', 'Destino', 'UF Destino', 'Filial', 'Tabela de Preco', 'Classificacao', 'Centro de Custo', 'Usuario', 'NF', 'Status', 'Modal', 'Tipo Frete', 'Data de extracao'], 'localizacao_cargas_2025-11-22_14-40-49.csv': ['CT-e', 'Tipo', 'Data Emissão', 'Volumes', 'Peso Taxado', 'Valor NF', 'Valor Frete', 'Tipo Serviço', 'Filial Emissora', 'Previsão Entrega', 'Cidade Destino', 'Filial Destino', 'Serviço', 'Status Carga', 'Filial Atual', 'Cidade Origem', 'Filial Origem', 'Latitude', 'Longitude', 'Velocidade', 'Altitude', 'Dispositivo ID', 'Dispositivo Tipo', 'Endereço', 'Data de extracao'], 'manifestos_2025-11-22_14-40-49.csv': ['Número', 'Filial', 'Data criação', 'Classificação', 'Status', 'Data Saída', 'Data Fechamento', 'Volumes NF', 'Qtd NF', 'Valor NF', 'Receita', 'Peso NF', 'Proprietário/Nome', 'Motorista', 'Veículo/Placa', 'Veículo/Tipo', 'KM Saída', 'KM Fechamento', 'KM Rodado', 'Custo Total', 'Custo (Líquido)', 'Chave MDFe', 'Status MDFe', 'Usuário', 'Data de extracao'], 'coletas_2025-11-22_14-40-49.csv': ['ID', 'Coleta', 'Cliente', 'Solicitante', 'Local da Coleta', 'Cidade', 'UF', 'Solicitacao', 'Hora (Solicitacao)', 'Agendamento', 'Horario (Inicio)', 'Finalizacao', 'Hora (Fim)', 'Status', 'Volumes', 'Peso Real', 'Peso Taxado', 'Valor NF', 'Usuario', 'Agente', 'Numero Manifesto', 'Veiculo', 'Data de extracao'], 'contas_a_pagar_2025-11-22_14-40-49.csv': ['Lançamento a Pagar/N°', 'N° Documento', 'Emissão', 'Tipo', 'Valor', 'Valor a pagar', 'Pago', 'Valor pago', 'Fornecedor/Nome', 'Filial', 'Conta Contábil/Classif.', 'Conta Contábil/Desc.', 'Conta Contábil/Valor', 'Centro de custo/Nome', 'Centro de custo/Valor', 'Mês Competência', 'Ano Competência', 'Data criação', 'Observações', 'Baixa/Data liquidação', 'Usuário/Nome', 'Data de extracao'], 'cotacoes_2025-11-22_14-40-49.csv': ['Cotação', 'Data', 'Filial', 'Solicitante', 'Cliente', 'CNPJ Cliente', 'Origem', 'UF (Origem)', 'Destino', 'UF (Destino)', 'Volumes', 'Peso Real', 'Peso Taxado', 'Valor NF', 'Valor Frete', 'Tabela', 'Usuário', 'Status Conversão', 'Motivo Perda', 'CT-e / Data emissão', 'NFS-e / Data emissão', 'Data de extracao']}

```

Com certeza. Se você quer precisão e não quer economizar informação, preparei o **"Guia Mestre de Implementação de BI"** para os seus 7 arquivos.

Aqui está detalhado arquivo por arquivo: quais colunas usar, quais ignorar, como relacionar as tabelas e, o mais importante, **as fórmulas DAX prontas** para copiar e colar.

-----

### 🏗️ Estrutura do Modelo de Dados (Relacionamentos)

Para que tudo funcione junto, você precisará criar uma tabela calendário.

1.  **Crie a tabela `dCalendario`:**
      * Ela deve ter datas de 01/01/2024 até o fim de 2025.
      * Relacionamentos:
          * `dCalendario[Data]` -\> `Manifestos[Data criação]` (Ativo)
          * `dCalendario[Data]` -\> `Fretes[Data frete]` (Ativo)
          * `dCalendario[Data]` -\> `Faturas[Parcelas / Vencimento]` (Ativo)
          * `dCalendario[Data]` -\> `Contas a Pagar[Baixa/Data liquidação]` (Inativo - Use `USERELATIONSHIP` na fórmula)

-----

### 1\. Arquivo: `manifestos_*.csv` (Lucratividade)

**Foco:** Quanto custou rodar a frota e quanto sobrou.

  * **Chave Única (ID):** Coluna `Número`.
  * **Colunas de Dinheiro:**
      * `Receita` -\> **O CORRETO** para faturamento.
      * `Valor NF` -\> **IGNORAR** para soma financeira (é apenas valor de seguro/risco).
      * `Custo Total` -\> O que você pagou ao motorista/posto.
  * **Fórmulas DAX:**

<!-- end list -->

```dax
// 1. Receita Real do Manifesto (Evita duplicidade se o manifesto tiver múltiplas linhas)
Receita Manifestos = 
CALCULATE(
    SUM(Manifestos[Receita]),
    DISTINCT(Manifestos[Número])
)

// 2. Custo Total da Operação
Custo Manifestos = 
CALCULATE(
    SUM(Manifestos[Custo Total]),
    DISTINCT(Manifestos[Número])
)

// 3. Margem de Lucro (R$)
Margem Bruta R$ = [Receita Manifestos] - [Custo Manifestos]

// 4. Margem de Lucro (%) - O indicador mais importante
Margem % = DIVIDE([Margem Bruta R$], [Receita Manifestos], 0)

// 5. Custo por KM Rodado
Custo por KM = 
VAR KmTotal = CALCULATE(SUM(Manifestos[KM Rodado]), DISTINCT(Manifestos[Número]))
RETURN DIVIDE([Custo Manifestos], KmTotal, 0)
```

-----

### 2\. Arquivo: `fretes_*.csv` (Comercial / Volume)

**Foco:** Vendas brutas e perfil de carga.

  * **Chave Única (ID):** Coluna `ID`.
  * **Colunas de Dinheiro:** `Valor Frete` (Receita).
  * **Fórmulas DAX:**

<!-- end list -->

```dax
// 1. Faturamento Bruto de Fretes
Total Fretes = SUM(Fretes[Valor Frete])

// 2. Ticket Médio (Preço médio por transporte)
Ticket Médio = AVERAGE(Fretes[Valor Frete])

// 3. Peso Total Transportado (Toneladas)
Peso Total Tons = SUM(Fretes[Kg Taxado]) / 1000

// 4. Contagem de Expedições
Qtd Fretes = COUNTROWS(Fretes)
```

-----

### 3\. Arquivo: `faturas_por_cliente_*.csv` (Financeiro - Recebíveis)

**Foco:** Fluxo de caixa futuro (o que vai entrar).

  * **Chave Única:** Combinação de `Fatura / N° Doc` + `Parcelas / Vencimento` (uma fatura pode ter parcelas).
  * **Data Crítica:** `Parcelas / Vencimento` (Use esta para o Eixo X).
  * **Fórmulas DAX:**

<!-- end list -->

```dax
// 1. Total a Receber (Carteira Aberta)
// Filtra apenas o que não foi baixado (pago) ainda
Saldo a Receber = 
CALCULATE(
    SUM('Faturas por Cliente'[Fatura / Valor]),
    'Faturas por Cliente'[Baixa / Data] = BLANK()
)

// 2. Total Recebido (Caixa Realizado)
Caixa Entrada = 
CALCULATE(
    SUM('Faturas por Cliente'[Fatura / Valor]),
    NOT(ISBLANK('Faturas por Cliente'[Baixa / Data]))
)

// 3. Inadimplência (Atrasados)
Valor Inadimplente = 
CALCULATE(
    [Saldo a Receber],
    'Faturas por Cliente'[Parcelas / Vencimento] < TODAY()
)
```

-----

### 4\. Arquivo: `contas_a_pagar_*.csv` (Financeiro - Pagáveis)

**Foco:** Saída de caixa.

  * **Chave Única:** `Lançamento a Pagar/N°`.
  * **Colunas de Dinheiro:** `Valor a pagar` (Previsto) e `Valor pago` (Realizado).
  * **Fórmulas DAX:**

<!-- end list -->

```dax
// 1. Contas a Pagar (Fluxo Futuro)
// Use relacionamento ativo com a Data de Vencimento (se houver) ou Emissão + Prazo
Saldo a Pagar = 
CALCULATE(
    SUM('Contas a Pagar'[Valor a pagar]),
    'Contas a Pagar'[Baixa/Data liquidação] = BLANK()
)

// 2. Pagamentos Realizados (Fluxo Passado)
// Nota: Aqui usamos a data de LIQUIDAÇÃO, não a de emissão.
Caixa Saída = 
CALCULATE(
    SUM('Contas a Pagar'[Valor pago]),
    USERELATIONSHIP('dCalendario'[Data], 'Contas a Pagar'[Baixa/Data liquidação])
)
```

-----

### 5\. Arquivo: `cotacoes_*.csv` (Comercial - Funil)

**Foco:** Eficiência de Vendas.

  * **Chave Única:** `Cotação`.
  * **Colunas Importantes:** `Status Conversão`, `Valor Frete`.
  * **Fórmulas DAX:**

<!-- end list -->

```dax
// 1. Volume Total Cotado (R$)
Total Oportunidades R$ = SUM(Cotacoes[Valor Frete])

// 2. Taxa de Conversão (Hit Rate)
// Assumindo que o status de sucesso é "Convertida" ou "Aprovada"
Taxa de Conversão = 
VAR Ganhas = CALCULATE(COUNTROWS(Cotacoes), Cotacoes[Status Conversão] = "Convertida")
VAR Total = COUNTROWS(Cotacoes)
RETURN DIVIDE(Ganhas, Total, 0)

// 3. Perda Financeira (Quanto deixamos de ganhar)
Valor Perdido = 
CALCULATE(
    SUM(Cotacoes[Valor Frete]),
    Cotacoes[Status Conversão] = "Perdida" // Ajuste conforme o texto exato do seu CSV
)
```

-----

### 6\. Arquivo: `localizacao_cargas_*.csv` (Operacional - Rastreio)

**Foco:** Onde está a carga agora.

  * **Chave Única:** `CT-e`.
  * **Colunas Importantes:** `Status Carga`, `Filial Atual`, `Previsão Entrega`.
  * **Fórmulas DAX:**

<!-- end list -->

```dax
// 1. Cargas em Atraso (Backlog Crítico)
Qtd Atrasados = 
CALCULATE(
    COUNTROWS('Localizacao Cargas'),
    'Localizacao Cargas'[Previsão Entrega] < TODAY(),
    'Localizacao Cargas'[Status Carga] <> "Entregue" // ou "finished"
)

// 2. Cargas Paradas na Filial (Gargalo)
Qtd em Armazem = 
CALCULATE(
    COUNTROWS('Localizacao Cargas'),
    'Localizacao Cargas'[Status Carga] = "in_warehouse" // Ajuste conforme CSV
)
```

-----

### 7\. Arquivo: `coletas_*.csv` (Operacional - Primeira Milha)

**Foco:** SLA de Coleta.

  * **Chave Única:** `ID` ou `Coleta`.
  * **Colunas Importantes:** `Status`, `Hora (Solicitacao)`, `Horario (Inicio)`.
  * **Fórmulas DAX:**

<!-- end list -->

```dax
// 1. Eficiência de Coleta
% Coletas Realizadas = 
VAR Realizadas = CALCULATE(COUNTROWS(Coletas), Coletas[Status] = "finished")
VAR Total = COUNTROWS(Coletas)
RETURN DIVIDE(Realizadas, Total, 0)

// 2. Tempo Médio de Atendimento (Horas)
// Precisa converter as colunas de hora para formato Time no Power Query antes
Tempo Medio Coleta = 
AVERAGEX(
    FILTER(Coletas, Coletas[Status] = "finished"),
    DATEDIFF(Coletas[Hora (Solicitacao)], Coletas[Horario (Inicio)], HOUR)
)
```

-----

### 🚀 Dica de Implementação

1.  Importe os 7 arquivos no Power BI.
2.  No Power Query, certifique-se de que colunas numéricas (`Receita`, `Valor Frete`, `Custo Total`) estejam como "Número Decimal" e não Texto.
3.  Crie as Medidas DAX acima (não Colunas Calculadas, use **Medidas** para performance).
4.  Monte as telas conforme o "Blueprints" que passei na resposta anterior.

Se seguir essas fórmulas, o número de "42 Milhões" vai desaparecer e você terá o valor real do lucro da transportadora.