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
# 📊 POWER BI - MODELO COMPLETO ADAPTADO PARA APIs

**Data:** 27/11/2025  
**Modelo Antigo:** Teste TI (214 medidas, 21 tabelas)  
**Modelo Novo:** ESL Cloud Analytics (APIs GraphQL + Data Export)

---

## 📋 ÍNDICE

1. [Estrutura do Modelo](#estrutura-do-modelo)
2. [Tabelas e Relacionamentos](#tabelas-e-relacionamentos)
3. [Medidas DAX por Área](#medidas-dax-por-área)
   - [Calendário](#calendário)
   - [Cotações](#cotações)
   - [Fretes](#fretes)
   - [Coletas](#coletas)
   - [Manifestos](#manifestos)
   - [Contas a Pagar](#contas-a-pagar)
   - [Faturas](#faturas)
   - [KPIs e Comparações](#kpis-e-comparações)
4. [Colunas Calculadas](#colunas-calculadas)
5. [Hierarquias](#hierarquias)
6. [Formatações](#formatações)

---

## 🏗️ ESTRUTURA DO MODELO

### Modelo Estrela (Star Schema)

```
                    dCalendario (Dimensão)
                           |
      ┌────────────────────┼────────────────────┐
      |                    |                    |
  fCotacoes            fFretes            fManifestos
  (Fatos)              (Fatos)             (Fatos)
      |                    |                    |
      └────────────────────┴────────────────────┘
                           |
                  Tabela de Medidas
```

### Tabelas do Novo Modelo

| Tabela | Tipo | Source | Descrição |
|--------|------|--------|-----------|
| **dCalendario** | Dimensão | DAX | Tabela calendário 2024-2030 |
| **fCotacoes** | Fato | SQL (API Data Export) | Template 6906 |
| **fFretes** | Fato | SQL (API GraphQL) | Query BuscarFretesExpandidaV3 |
| **fColetas** | Fato | SQL (API GraphQL) | Query BuscarColetasExpandidaV2 |
| **fManifestos** | Fato | SQL (API Data Export) | Template 6399 |
| **fContasAPagar** | Fato | SQL (API Data Export) | Template 8636 |
| **fFaturasCliente** | Fato | SQL (API Data Export) | Template 4924 |
| **fLocalizacaoCargas** | Fato | SQL (API Data Export) | Template 8656 |
| **Medidas** | Medidas | DAX | Tabela para organizar medidas |

---

## 🔗 TABELAS E RELACIONAMENTOS

### 1. Criar Tabela de Calendário

```dax
dCalendario = 
CALENDAR(
    DATE(2024, 1, 1),
    DATE(2030, 12, 31)
)
```

### 2. Criar Tabela de Medidas

```dax
Medidas = ROW("Medidas", 1)
```

### 3. Relacionamentos Principais

```
dCalendario[Data] (1) → (*)fCotacoes[requested_at]
dCalendario[Data] (1) → (*)fFretes[servico_em]
dCalendario[Data] (1) → (*)fColetas[request_date]
dCalendario[Data] (1) → (*)fManifestos[created_at]
dCalendario[Data] (1) → (*)fContasAPagar[issue_date]
dCalendario[Data] (1) → (*)fFaturasCliente[fit_fhe_cte_issued_at]
```

**Importante:** 
- Todos os relacionamentos são **1:*** (um para muitos)
- Direção de filtro: **Ambas** ou **Single** (ajustar conforme necessidade)
- Data ativa: campo de data principal de cada tabela

---

## 📅 CALENDÁRIO - Colunas Calculadas

### Colunas Básicas

```dax
// 1. Ano
Ano = YEAR(dCalendario[Data])

// 2. Mês (Número)
MesNumero = MONTH(dCalendario[Data])

// 3. Mês (Nome)
MesNome = FORMAT(dCalendario[Data], "MMMM")

// 4. Mês Abreviado
MesAbreviado = FORMAT(dCalendario[Data], "MMM")

// 5. Mês-Ano
MesAno = FORMAT(dCalendario[Data], "MMM/YYYY")

// 6. Trimestre
Trimestre = "T" & FORMAT(dCalendario[Data], "Q")

// 7. Dia da Semana
DiaSemana = FORMAT(dCalendario[Data], "dddd")

// 8. Dia da Semana (Número)
DiaSemanaNumero = WEEKDAY(dCalendario[Data])

// 9. Dia do Ano
DiaAno = DATEDIFF(DATE(YEAR(dCalendario[Data]), 1, 1), dCalendario[Data], DAY) + 1

// 10. Semana do Ano
SemanaAno = WEEKNUM(dCalendario[Data])

// 11. É Fim de Semana
IsFimDeSemana = IF(WEEKDAY(dCalendario[Data]) IN {1, 7}, "Sim", "Não")

// 12. Ano Fiscal (Considerando que o ano fiscal começa em Janeiro)
AnoFiscal = YEAR(dCalendario[Data])
```

### Hierarquias

```dax
// Criar Hierarquia de Data
Ano → Trimestre → MesNome → Data

// Criar Hierarquia de Semana
Ano → SemanaAno → Data
```

---

## 💰 COTAÇÕES - Medidas DAX

### Medidas Básicas

```dax
// ══════════════════════════════════════════════════════════════
// COTAÇÕES - MEDIDAS BÁSICAS
// ══════════════════════════════════════════════════════════════

// 1. Total de Cotações
COT_Realizadas Qtde = 
COUNT(fCotacoes[sequence_code])

// 2. Valor Total de Cotações
COT_Realizadas Valor = 
SUM(fCotacoes[total_value])

// 3. Cotações Convertidas (Quantidade)
COT_Convertidas Qtde = 
CALCULATE(
    COUNT(fCotacoes[sequence_code]),
    fCotacoes[fit_fhe_cte_issued_at] <> BLANK()
)

// 4. Cotações Convertidas (Valor)
COT_Convertidas Valor = 
CALCULATE(
    SUM(fCotacoes[total_value]),
    fCotacoes[fit_fhe_cte_issued_at] <> BLANK()
)

// 5. Cotações Pendentes (Quantidade)
COT_Pendentes Qtde = 
CALCULATE(
    COUNT(fCotacoes[sequence_code]),
    fCotacoes[fit_fhe_cte_issued_at] = BLANK()
)

// 6. Cotações Pendentes (Valor)
COT_Pendentes Valor = 
CALCULATE(
    SUM(fCotacoes[total_value]),
    fCotacoes[fit_fhe_cte_issued_at] = BLANK()
)

// 7. Peso Taxado Total
COT_Peso Taxado Total = 
SUM(fCotacoes[taxed_weight])

// 8. Peso Real Total
COT_Peso Real Total = 
SUM(fCotacoes[real_weight])

// 9. Valor das Notas Fiscais
COT_Valor NF Total = 
SUM(fCotacoes[invoices_value])

// 10. Volumes Total
COT_Volumes Total = 
SUM(fCotacoes[volumes])
```

### Medidas de Taxa de Conversão

```dax
// ══════════════════════════════════════════════════════════════
// COTAÇÕES - TAXAS DE CONVERSÃO
// ══════════════════════════════════════════════════════════════

// 11. Taxa de Conversão (Quantidade)
COT_Taxa Conversao Qtde = 
DIVIDE(
    [COT_Convertidas Qtde],
    [COT_Realizadas Qtde],
    0
)

// 12. Taxa de Conversão (Valor)
COT_Taxa Conversao Valor = 
DIVIDE(
    [COT_Convertidas Valor],
    [COT_Realizadas Valor],
    0
)

// 13. Kg Médio por Cotação
COT_Kg Medio = 
DIVIDE(
    [COT_Peso Taxado Total],
    [COT_Realizadas Qtde],
    0
)

// 14. Valor Médio por Kg
COT_Valor Medio por Kg = 
DIVIDE(
    [COT_Realizadas Valor],
    [COT_Peso Taxado Total],
    0
)

// 15. Ticket Médio de Cotação
COT_Ticket Medio = 
DIVIDE(
    [COT_Realizadas Valor],
    [COT_Realizadas Qtde],
    0
)
```

### Medidas de Análise Temporal (Ano Anterior)

```dax
// ══════════════════════════════════════════════════════════════
// COTAÇÕES - ANO ANTERIOR (YOY)
// ══════════════════════════════════════════════════════════════

// 16. Cotações Realizadas Qtde - Ano Anterior
COT_Realizadas Qtde AA = 
CALCULATE(
    [COT_Realizadas Qtde],
    DATEADD(dCalendario[Data], -1, YEAR)
)

// 17. Cotações Realizadas Valor - Ano Anterior
COT_Realizadas Valor AA = 
CALCULATE(
    [COT_Realizadas Valor],
    DATEADD(dCalendario[Data], -1, YEAR)
)

// 18. Cotações Convertidas Qtde - Ano Anterior
COT_Convertidas Qtde AA = 
CALCULATE(
    [COT_Convertidas Qtde],
    DATEADD(dCalendario[Data], -1, YEAR)
)

// 19. Cotações Convertidas Valor - Ano Anterior
COT_Convertidas Valor AA = 
CALCULATE(
    [COT_Convertidas Valor],
    DATEADD(dCalendario[Data], -1, YEAR)
)

// 20. Taxa de Conversão Valor - Ano Anterior
COT_Taxa Conversao Valor AA = 
DIVIDE(
    [COT_Convertidas Valor AA],
    [COT_Realizadas Valor AA],
    0
)
```

### Medidas de Variação (YOY)

```dax
// ══════════════════════════════════════════════════════════════
// COTAÇÕES - VARIAÇÃO ANO A ANO
// ══════════════════════════════════════════════════════════════

// 21. Variação Qtde (YOY %)
COT_Var Qtde YOY = 
VAR _atual = [COT_Realizadas Qtde]
VAR _anterior = [COT_Realizadas Qtde AA]
RETURN
    IF(
        _anterior = 0 || ISBLANK(_anterior),
        BLANK(),
        DIVIDE(_atual - _anterior, _anterior)
    )

// 22. Variação Valor (YOY %)
COT_Var Valor YOY = 
VAR _atual = [COT_Realizadas Valor]
VAR _anterior = [COT_Realizadas Valor AA]
RETURN
    IF(
        _anterior = 0 || ISBLANK(_anterior),
        BLANK(),
        DIVIDE(_atual - _anterior, _anterior)
    )

// 23. Variação Taxa Conversão (YOY pp)
COT_Var Taxa Conversao = 
[COT_Taxa Conversao Valor] - [COT_Taxa Conversao Valor AA]

// 24. Variação Absoluta Qtde (YOY)
COT_Var Abs Qtde YOY = 
[COT_Realizadas Qtde] - [COT_Realizadas Qtde AA]

// 25. Variação Absoluta Valor (YOY)
COT_Var Abs Valor YOY = 
[COT_Realizadas Valor] - [COT_Realizadas Valor AA]
```

### Medidas de Clientes e Usuários

```dax
// ══════════════════════════════════════════════════════════════
// COTAÇÕES - CLIENTES E USUÁRIOS
// ══════════════════════════════════════════════════════════════

// 26. Clientes Únicos
COT_Clientes Unicos = 
DISTINCTCOUNT(fCotacoes[customer_name])

// 27. Usuários Únicos
COT_Usuarios Unicos = 
DISTINCTCOUNT(fCotacoes[user_name])

// 28. Filiais Únicas
COT_Filiais Unicas = 
DISTINCTCOUNT(fCotacoes[branch_nickname])

// 29. Cotações por Cliente
COT_Qtde por Cliente = 
DIVIDE(
    [COT_Realizadas Qtde],
    [COT_Clientes Unicos],
    0
)

// 30. Cotações por Usuário
COT_Qtde por Usuario = 
DIVIDE(
    [COT_Realizadas Qtde],
    [COT_Usuarios Unicos],
    0
)
```

### Medidas de Origem e Destino

```dax
// ══════════════════════════════════════════════════════════════
// COTAÇÕES - ORIGEM E DESTINO
// ══════════════════════════════════════════════════════════════

// 31. Cidades de Origem Únicas
COT_Cidades Origem Unicas = 
DISTINCTCOUNT(fCotacoes[origin_city])

// 32. Cidades de Destino Únicas
COT_Cidades Destino Unicas = 
DISTINCTCOUNT(fCotacoes[destination_city])

// 33. Estados de Origem Únicos
COT_Estados Origem Unicos = 
DISTINCTCOUNT(fCotacoes[origin_state])

// 34. Estados de Destino Únicos
COT_Estados Destino Unicos = 
DISTINCTCOUNT(fCotacoes[destination_state])

// 35. Rotas Únicas (Origem → Destino)
COT_Rotas Unicas = 
COUNTROWS(
    SUMMARIZE(
        fCotacoes,
        fCotacoes[origin_city],
        fCotacoes[origin_state],
        fCotacoes[destination_city],
        fCotacoes[destination_state]
    )
)
```

---

## 🚚 FRETES - Medidas DAX

### Medidas Básicas

```dax
// ══════════════════════════════════════════════════════════════
// FRETES - MEDIDAS BÁSICAS
// ══════════════════════════════════════════════════════════════

// 1. Total de Fretes
FRE_Fretes Qtde = 
COUNT(fFretes[id])

// 2. Valor Total de Fretes
FRE_Valor Total = 
SUM(fFretes[valor_total])

// 3. Valor das Notas Fiscais
FRE_Valor NF = 
SUM(fFretes[valor_notas])

// 4. Peso das Notas (Kg)
FRE_Peso NF = 
SUM(fFretes[peso_notas])

// 5. Volumes Total
FRE_Volumes Total = 
SUM(fFretes[metadata.invoicesTotalVolumes]) // Do JSON

// 6. Peso Taxado
FRE_Peso Taxado = 
SUM(fFretes[metadata.taxedWeight]) // Do JSON

// 7. Peso Real
FRE_Peso Real = 
SUM(fFretes[metadata.realWeight]) // Do JSON

// 8. Cubagem Total (M³)
FRE_Cubagem Total = 
SUM(fFretes[metadata.totalCubicVolume]) // Do JSON
```

### Medidas por Status

```dax
// ══════════════════════════════════════════════════════════════
// FRETES - STATUS
// ══════════════════════════════════════════════════════════════

// 9. Fretes Finalizados
FRE_Finalizados Qtde = 
CALCULATE(
    [FRE_Fretes Qtde],
    fFretes[status] = "finished"
)

// 10. Fretes Pendentes
FRE_Pendentes Qtde = 
CALCULATE(
    [FRE_Fretes Qtde],
    fFretes[status] = "pending"
)

// 11. Fretes Em Entrega
FRE_EmEntrega Qtde = 
CALCULATE(
    [FRE_Fretes Qtde],
    fFretes[status] = "delivering"
)

// 12. Fretes Cancelados
FRE_Cancelados Qtde = 
CALCULATE(
    [FRE_Fretes Qtde],
    fFretes[status] = "cancelled"
)

// 13. Taxa de Conclusão
FRE_Taxa Conclusao = 
DIVIDE(
    [FRE_Finalizados Qtde],
    [FRE_Fretes Qtde],
    0
)
```

### Medidas de Análise

```dax
// ══════════════════════════════════════════════════════════════
// FRETES - ANÁLISES
// ══════════════════════════════════════════════════════════════

// 14. Ticket Médio
FRE_Ticket Medio = 
DIVIDE(
    [FRE_Valor Total],
    [FRE_Fretes Qtde],
    0
)

// 15. Valor Médio por Kg
FRE_Valor por Kg = 
DIVIDE(
    [FRE_Valor Total],
    [FRE_Peso Taxado],
    0
)

// 16. Kg Médio por Frete
FRE_Kg Medio = 
DIVIDE(
    [FRE_Peso Taxado],
    [FRE_Fretes Qtde],
    0
)

// 17. % Frete/NF
FRE_Percentual FreteSobreNF = 
DIVIDE(
    [FRE_Valor Total],
    [FRE_Valor NF],
    0
)

// 18. Densidade Média (Kg/M³)
FRE_Densidade Media = 
DIVIDE(
    [FRE_Peso Real],
    [FRE_Cubagem Total],
    0
)
```

### Medidas de Clientes e Localidades

```dax
// ══════════════════════════════════════════════════════════════
// FRETES - CLIENTES E LOCALIDADES
// ══════════════════════════════════════════════════════════════

// 19. Pagadores Únicos
FRE_Pagadores Unicos = 
DISTINCTCOUNT(fFretes[pagador_id])

// 20. Remetentes Únicos
FRE_Remetentes Unicos = 
DISTINCTCOUNT(fFretes[remetente_id])

// 21. Destinatários Únicos
FRE_Destinatarios Unicos = 
DISTINCTCOUNT(fFretes[destinatario_id])

// 22. Cidades Origem Únicas
FRE_Cidades Origem = 
DISTINCTCOUNT(fFretes[origem_cidade])

// 23. Cidades Destino Únicas
FRE_Cidades Destino = 
DISTINCTCOUNT(fFretes[destino_cidade])

// 24. Rotas Únicas
FRE_Rotas Unicas = 
COUNTROWS(
    SUMMARIZE(
        fFretes,
        fFretes[origem_cidade],
        fFretes[origem_uf],
        fFretes[destino_cidade],
        fFretes[destino_uf]
    )
)
```

### Medidas Temporais (YOY)

```dax
// ══════════════════════════════════════════════════════════════
// FRETES - ANO ANTERIOR
// ══════════════════════════════════════════════════════════════

// 25. Fretes Qtde - Ano Anterior
FRE_Fretes Qtde AA = 
CALCULATE(
    [FRE_Fretes Qtde],
    DATEADD(dCalendario[Data], -1, YEAR)
)

// 26. Valor Total - Ano Anterior
FRE_Valor Total AA = 
CALCULATE(
    [FRE_Valor Total],
    DATEADD(dCalendario[Data], -1, YEAR)
)

// 27. Variação Qtde (YOY %)
FRE_Var Qtde YOY = 
VAR _atual = [FRE_Fretes Qtde]
VAR _anterior = [FRE_Fretes Qtde AA]
RETURN
    IF(
        _anterior = 0 || ISBLANK(_anterior),
        BLANK(),
        DIVIDE(_atual - _anterior, _anterior)
    )

// 28. Variação Valor (YOY %)
FRE_Var Valor YOY = 
VAR _atual = [FRE_Valor Total]
VAR _anterior = [FRE_Valor Total AA]
RETURN
    IF(
        _anterior = 0 || ISBLANK(_anterior),
        BLANK(),
        DIVIDE(_atual - _anterior, _anterior)
    )
```

---

## 📦 COLETAS - Medidas DAX

### Medidas Básicas

```dax
// ══════════════════════════════════════════════════════════════
// COLETAS - MEDIDAS BÁSICAS
// ══════════════════════════════════════════════════════════════

// 1. Total de Coletas
COL_Coletas Qtde = 
COUNT(fColetas[id])

// 2. Valor Total das NFs
COL_Valor Total = 
SUM(fColetas[total_value])

// 3. Peso Taxado Total
COL_Peso Taxado = 
SUM(fColetas[taxed_weight])

// 4. Volumes Total
COL_Volumes Total = 
SUM(fColetas[total_volumes])

// 5. Peso Real Total (do metadata)
COL_Peso Real = 
SUMX(
    fColetas,
    VALUE(PATHITEM(fColetas[metadata], "invoicesWeight"))
)
```

### Medidas por Status

```dax
// ══════════════════════════════════════════════════════════════
// COLETAS - STATUS
// ══════════════════════════════════════════════════════════════

// 6. Coletas Finalizadas
COL_Finalizadas Qtde = 
CALCULATE(
    [COL_Coletas Qtde],
    fColetas[status] = "finished"
)

// 7. Coletas Pendentes
COL_Pendentes Qtde = 
CALCULATE(
    [COL_Coletas Qtde],
    fColetas[status] IN {"pending", "created"}
)

// 8. Coletas Manifestadas
COL_Manifestadas Qtde = 
CALCULATE(
    [COL_Coletas Qtde],
    fColetas[status] = "manifested"
)

// 9. Coletas Em Trânsito
COL_EmTransito Qtde = 
CALCULATE(
    [COL_Coletas Qtde],
    fColetas[status] = "in_transit"
)

// 10. Coletas Canceladas
COL_Canceladas Qtde = 
CALCULATE(
    [COL_Coletas Qtde],
    fColetas[status] = "cancelled"
)

// 11. Taxa de Conclusão
COL_Taxa Conclusao = 
DIVIDE(
    [COL_Finalizadas Qtde],
    [COL_Coletas Qtde],
    0
)

// 12. Taxa de Cancelamento
COL_Taxa Cancelamento = 
DIVIDE(
    [COL_Canceladas Qtde],
    [COL_Coletas Qtde],
    0
)
```

### Medidas de Performance

```dax
// ══════════════════════════════════════════════════════════════
// COLETAS - PERFORMANCE
// ══════════════════════════════════════════════════════════════

// 13. Tempo Médio Solicitação → Coleta (Horas)
COL_Tempo Medio Horas = 
AVERAGEX(
    FILTER(
        fColetas,
        fColetas[finish_date] <> BLANK()
    ),
    DATEDIFF(
        fColetas[request_date],
        fColetas[finish_date],
        HOUR
    )
)

// 14. Coletas no Prazo
COL_NoPrazo Qtde = 
CALCULATE(
    [COL_Coletas Qtde],
    fColetas[finish_date] <= fColetas[service_date]
)

// 15. Coletas Atrasadas
COL_Atrasadas Qtde = 
CALCULATE(
    [COL_Coletas Qtde],
    fColetas[finish_date] > fColetas[service_date]
)

// 16. Taxa de Pontualidade
COL_Taxa Pontualidade = 
DIVIDE(
    [COL_NoPrazo Qtde],
    [COL_Finalizadas Qtde],
    0
)

// 17. Kg Médio por Coleta
COL_Kg Medio = 
DIVIDE(
    [COL_Peso Taxado],
    [COL_Coletas Qtde],
    0
)

// 18. Valor Médio por Coleta
COL_Valor Medio = 
DIVIDE(
    [COL_Valor Total],
    [COL_Coletas Qtde],
    0
)
```

### Medidas de Clientes e Usuários

```dax
// ══════════════════════════════════════════════════════════════
// COLETAS - CLIENTES E USUÁRIOS
// ══════════════════════════════════════════════════════════════

// 19. Clientes Únicos
COL_Clientes Unicos = 
DISTINCTCOUNT(fColetas[cliente_id])

// 20. Usuários Únicos
COL_Usuarios Unicos = 
DISTINCTCOUNT(fColetas[usuario_id])

// 21. Cidades Únicas
COL_Cidades Unicas = 
DISTINCTCOUNT(fColetas[cidade_coleta])

// 22. Coletas por Cliente
COL_Qtde por Cliente = 
DIVIDE(
    [COL_Coletas Qtde],
    [COL_Clientes Unicos],
    0
)

// 23. Coletas por Usuário
COL_Qtde por Usuario = 
DIVIDE(
    [COL_Coletas Qtde],
    [COL_Usuarios Unicos],
    0
)
```

---

## 📋 MANIFESTOS - Medidas DAX

### Medidas Básicas

```dax
// ══════════════════════════════════════════════════════════════
// MANIFESTOS - MEDIDAS BÁSICAS
// ══════════════════════════════════════════════════════════════

// 1. Total de Manifestos
MAN_Manifestos Qtde = 
COUNT(fManifestos[sequence_code])

// 2. Valor Total das Notas
MAN_Valor Notas = 
SUM(fManifestos[invoices_value])

// 3. Peso Notas (Kg)
MAN_Peso Notas = 
SUM(fManifestos[invoices_weight])

// 4. Peso Taxado Total
MAN_Peso Taxado = 
SUM(fManifestos[total_taxed_weight])

// 5. Cubagem Total (M³)
MAN_Cubagem Total = 
SUM(fManifestos[total_cubic_volume])

// 6. Volumes Total
MAN_Volumes Total = 
SUM(fManifestos[invoices_volumes])

// 7. Quantidade de Notas
MAN_Notas Qtde = 
SUM(fManifestos[invoices_count])

// 8. Valor Fretes do Manifesto
MAN_Valor Fretes = 
SUM(fManifestos[manifest_freights_total])

// 9. KM Rodado
MAN_KM Rodado = 
SUM(fManifestos[traveled_km])
```

### Medidas de Custos

```dax
// ══════════════════════════════════════════════════════════════
// MANIFESTOS - CUSTOS
// ══════════════════════════════════════════════════════════════

// 10. Custo Total
MAN_Custo Total = 
SUM(fManifestos[total_cost])

// 11. Diárias
MAN_Diarias = 
SUM(fManifestos[daily_subtotal])

// 12. Despesas Operacionais
MAN_Despesas Operacionais = 
SUM(fManifestos[operational_expenses_total])

// 13. INSS
MAN_INSS = 
SUM(fManifestos[inss_value])

// 14. SEST/SENAT
MAN_SEST_SENAT = 
SUM(fManifestos[sest_senat_value])

// 15. IR
MAN_IR = 
SUM(fManifestos[ir_value])

// 16. Valor a Pagar
MAN_Valor Pagar = 
SUM(fManifestos[paying_total])
```

### Medidas de Eficiência

```dax
// ══════════════════════════════════════════════════════════════
// MANIFESTOS - EFICIÊNCIA
// ══════════════════════════════════════════════════════════════

// 17. Rentabilidade (%)
MAN_Rentabilidade = 
DIVIDE(
    [MAN_Valor Fretes] - [MAN_Custo Total],
    [MAN_Valor Fretes],
    0
)

// 18. Custo por KM
MAN_Custo por KM = 
DIVIDE(
    [MAN_Custo Total],
    [MAN_KM Rodado],
    0
)

// 19. Receita por KM
MAN_Receita por KM = 
DIVIDE(
    [MAN_Valor Fretes],
    [MAN_KM Rodado],
    0
)

// 20. Kg por Manifesto
MAN_Kg por Manifesto = 
DIVIDE(
    [MAN_Peso Taxado],
    [MAN_Manifestos Qtde],
    0
)

// 21. Valor por Manifesto
MAN_Valor por Manifesto = 
DIVIDE(
    [MAN_Valor Fretes],
    [MAN_Manifestos Qtde],
    0
)

// 22. Notas por Manifesto
MAN_Notas por Manifesto = 
DIVIDE(
    [MAN_Notas Qtde],
    [MAN_Manifestos Qtde],
    0
)

// 23. % Custo sobre Receita
MAN_Custo Percentual = 
DIVIDE(
    [MAN_Custo Total],
    [MAN_Valor Fretes],
    0
)
```

### Medidas por Status

```dax
// ══════════════════════════════════════════════════════════════
// MANIFESTOS - STATUS
// ══════════════════════════════════════════════════════════════

// 24. Manifestos Em Trânsito
MAN_EmTransito Qtde = 
CALCULATE(
    [MAN_Manifestos Qtde],
    fManifestos[status] = "in_transit"
)

// 25. Manifestos Fechados
MAN_Fechados Qtde = 
CALCULATE(
    [MAN_Manifestos Qtde],
    fManifestos[status] = "closed"
)

// 26. Manifestos Finalizados
MAN_Finalizados Qtde = 
CALCULATE(
    [MAN_Manifestos Qtde],
    fManifestos[status] = "finished"
)

// 27. Taxa de Finalização
MAN_Taxa Finalizacao = 
DIVIDE(
    [MAN_Finalizados Qtde],
    [MAN_Manifestos Qtde],
    0
)
```

### Medidas por Classificação

```dax
// ══════════════════════════════════════════════════════════════
// MANIFESTOS - CLASSIFICAÇÃO
// ══════════════════════════════════════════════════════════════

// 28. Manifestos Distribuição
MAN_Distribuicao Qtde = 
CALCULATE(
    [MAN_Manifestos Qtde],
    fManifestos[classification] = "DISTRIBUIÇÃO"
)

// 29. Manifestos Transferência
MAN_Transferencia Qtde = 
CALCULATE(
    [MAN_Manifestos Qtde],
    fManifestos[classification] = "TRANSFERÊNCIA"
)

// 30. Manifestos Coleta
MAN_Coleta Qtde = 
CALCULATE(
    [MAN_Manifestos Qtde],
    fManifestos[classification] = "COLETA"
)

// 31. Rentabilidade Distribuição
MAN_Rent_Distribuicao = 
CALCULATE(
    [MAN_Rentabilidade],
    fManifestos[classification] = "DISTRIBUIÇÃO"
)

// 32. Rentabilidade Transferência
MAN_Rent_Transferencia = 
CALCULATE(
    [MAN_Rentabilidade],
    fManifestos[classification] = "TRANSFERÊNCIA"
)

// 33. Custo% Distribuição
MAN_Custo_Distribuicao = 
CALCULATE(
    [MAN_Custo Percentual],
    fManifestos[classification] = "DISTRIBUIÇÃO"
)
```

---

## 💳 CONTAS A PAGAR - Medidas DAX

### Medidas Básicas

```dax
// ══════════════════════════════════════════════════════════════
// CONTAS A PAGAR - MEDIDAS BÁSICAS
// ══════════════════════════════════════════════════════════════

// 1. Total de Lançamentos
CAP_Lancamentos Qtde = 
COUNT(fContasAPagar[sequence_code])

// 2. Valor Original Total
CAP_Valor Original = 
SUM(fContasAPagar[valor_original])

// 3. Juros Total
CAP_Juros = 
SUM(fContasAPagar[valor_juros])

// 4. Desconto Total
CAP_Desconto = 
SUM(fContasAPagar[valor_desconto])

// 5. Valor a Pagar Total
CAP_Valor Pagar = 
SUM(fContasAPagar[valor_a_pagar])

// 6. Valor Pago Total
CAP_Valor Pago = 
SUM(fContasAPagar[valor_pago])

// 7. Saldo a Pagar
CAP_Saldo = 
[CAP_Valor Pagar] - [CAP_Valor Pago]
```

### Medidas por Status

```dax
// ══════════════════════════════════════════════════════════════
// CONTAS A PAGAR - STATUS
// ══════════════════════════════════════════════════════════════

// 8. Contas Pagas
CAP_Pagas Qtde = 
CALCULATE(
    [CAP_Lancamentos Qtde],
    fContasAPagar[status_pagamento] = "PAGO"
)

// 9. Contas Abertas
CAP_Abertas Qtde = 
CALCULATE(
    [CAP_Lancamentos Qtde],
    fContasAPagar[status_pagamento] = "ABERTO"
)

// 10. Valor Pago (Status PAGO)
CAP_Pago Status = 
CALCULATE(
    [CAP_Valor Pago],
    fContasAPagar[status_pagamento] = "PAGO"
)

// 11. Valor em Aberto (Status ABERTO)
CAP_Aberto Valor = 
CALCULATE(
    [CAP_Valor Pagar],
    fContasAPagar[status_pagamento] = "ABERTO"
)

// 12. Taxa de Pagamento (%)
CAP_Taxa Pagamento = 
DIVIDE(
    [CAP_Pagas Qtde],
    [CAP_Lancamentos Qtde],
    0
)
```

### Medidas por Tipo

```dax
// ══════════════════════════════════════════════════════════════
// CONTAS A PAGAR - TIPO DE LANÇAMENTO
// ══════════════════════════════════════════════════════════════

// 13. Fornecedores Únicos
CAP_Fornecedores Unicos = 
DISTINCTCOUNT(fContasAPagar[nome_fornecedor])

// 14. Filiais Únicas
CAP_Filiais Unicas = 
DISTINCTCOUNT(fContasAPagar[nome_filial])

// 15. Centros de Custo Únicos
CAP_CentroCusto Unicos = 
DISTINCTCOUNT(fContasAPagar[nome_centro_custo])

// 16. Valor Médio por Lançamento
CAP_Valor Medio = 
DIVIDE(
    [CAP_Valor Pagar],
    [CAP_Lancamentos Qtde],
    0
)
```

### Medidas por Competência

```dax
// ══════════════════════════════════════════════════════════════
// CONTAS A PAGAR - COMPETÊNCIA
// ══════════════════════════════════════════════════════════════

// 17. Valor por Mês Competência (Filtrado)
CAP_Competencia Mes = 
CALCULATE(
    [CAP_Valor Pagar],
    USERELATIONSHIP(fContasAPagar[data_competencia_calculada], dCalendario[Data])
)

// 18. Contas Vencidas (hoje > data_liquidacao AND status = ABERTO)
CAP_Vencidas Qtde = 
CALCULATE(
    [CAP_Lancamentos Qtde],
    fContasAPagar[status_pagamento] = "ABERTO",
    fContasAPagar[data_liquidacao] < TODAY()
)

// 19. Valor Vencido
CAP_Vencido Valor = 
CALCULATE(
    [CAP_Valor Pagar],
    fContasAPagar[status_pagamento] = "ABERTO",
    fContasAPagar[data_liquidacao] < TODAY()
)
```

---

## 🧾 FATURAS POR CLIENTE - Medidas DAX

### Medidas Básicas

```dax
// ══════════════════════════════════════════════════════════════
// FATURAS - MEDIDAS BÁSICAS
// ══════════════════════════════════════════════════════════════

// 1. Total de Faturas
FAT_Faturas Qtde = 
COUNT(fFaturasCliente[unique_id])

// 2. Valor Frete Total
FAT_Valor Frete = 
SUM(fFaturasCliente[valor_frete])

// 3. Valor Fatura Total
FAT_Valor Fatura = 
SUM(fFaturasCliente[valor_fatura])

// 4. CT-es Emitidos
FAT_CTes Qtde = 
COUNTROWS(
    FILTER(
        fFaturasCliente,
        fFaturasCliente[numero_cte] <> BLANK()
    )
)

// 5. NFS-es Emitidas
FAT_NFSes Qtde = 
COUNTROWS(
    FILTER(
        fFaturasCliente,
        fFaturasCliente[numero_nfse] <> BLANK()
    )
)
```

### Medidas por Status

```dax
// ══════════════════════════════════════════════════════════════
// FATURAS - STATUS CT-e
// ══════════════════════════════════════════════════════════════

// 6. CT-es Autorizados
FAT_CTes Autorizados = 
CALCULATE(
    [FAT_CTes Qtde],
    fFaturasCliente[status_cte] = "Autorizado"
)

// 7. CT-es Cancelados
FAT_CTes Cancelados = 
CALCULATE(
    [FAT_CTes Qtde],
    fFaturasCliente[status_cte] = "Cancelado"
)

// 8. Taxa de Cancelamento CT-e
FAT_Taxa Cancelamento CTe = 
DIVIDE(
    [FAT_CTes Cancelados],
    [FAT_CTes Qtde],
    0
)
```

### Medidas de Análise

```dax
// ══════════════════════════════════════════════════════════════
// FATURAS - ANÁLISES
// ══════════════════════════════════════════════════════════════

// 9. Pagadores Únicos
FAT_Pagadores Unicos = 
DISTINCTCOUNT(fFaturasCliente[pagador_nome])

// 10. Remetentes Únicos
FAT_Remetentes Unicos = 
DISTINCTCOUNT(fFaturasCliente[remetente_nome])

// 11. Destinatários Únicos
FAT_Destinatarios Unicos = 
DISTINCTCOUNT(fFaturasCliente[destinatario_nome])

// 12. Filiais Únicas
FAT_Filiais Unicas = 
DISTINCTCOUNT(fFaturasCliente[filial])

// 13. Vendedores Únicos
FAT_Vendedores Unicos = 
DISTINCTCOUNT(fFaturasCliente[vendedor_nome])

// 14. Ticket Médio Fatura
FAT_Ticket Medio = 
DIVIDE(
    [FAT_Valor Fatura],
    [FAT_Faturas Qtde],
    0
)
```

### Medidas de Faturamento

```dax
// ══════════════════════════════════════════════════════════════
// FATURAS - FATURAMENTO E BAIXA
// ══════════════════════════════════════════════════════════════

// 15. Faturas Baixadas (Pagas)
FAT_Baixadas Qtde = 
COUNTROWS(
    FILTER(
        fFaturasCliente,
        fFaturasCliente[data_baixa_fatura] <> BLANK()
    )
)

// 16. Faturas em Aberto
FAT_Abertas Qtde = 
COUNTROWS(
    FILTER(
        fFaturasCliente,
        fFaturasCliente[data_baixa_fatura] = BLANK()
    )
)

// 17. Valor Baixado
FAT_Valor Baixado = 
CALCULATE(
    [FAT_Valor Fatura],
    fFaturasCliente[data_baixa_fatura] <> BLANK()
)

// 18. Valor em Aberto
FAT_Valor Aberto = 
CALCULATE(
    [FAT_Valor Fatura],
    fFaturasCliente[data_baixa_fatura] = BLANK()
)

// 19. Taxa de Recebimento
FAT_Taxa Recebimento = 
DIVIDE(
    [FAT_Baixadas Qtde],
    [FAT_Faturas Qtde],
    0
)
```

---

## 📊 KPIS E COMPARAÇÕES

### KPIs Gerais

```dax
// ══════════════════════════════════════════════════════════════
// KPIs GERAIS
// ══════════════════════════════════════════════════════════════

// 1. Receita Total (Fretes + Faturas)
KPI_Receita Total = 
[FRE_Valor Total] + [FAT_Valor Fatura]

// 2. Custo Total (Manifestos + Contas a Pagar)
KPI_Custo Total = 
[MAN_Custo Total] + [CAP_Valor Pagar]

// 3. Lucro Bruto
KPI_Lucro Bruto = 
[KPI_Receita Total] - [KPI_Custo Total]

// 4. Margem Bruta (%)
KPI_Margem Bruta = 
DIVIDE(
    [KPI_Lucro Bruto],
    [KPI_Receita Total],
    0
)

// 5. Operações Total (Fretes + Coletas + Manifestos)
KPI_Operacoes Total = 
[FRE_Fretes Qtde] + [COL_Coletas Qtde] + [MAN_Manifestos Qtde]
```

### Comparações YOY Consolidadas

```dax
// ══════════════════════════════════════════════════════════════
// COMPARAÇÕES ANO A ANO (YOY)
// ══════════════════════════════════════════════════════════════

// 6. Receita Total - Ano Anterior
KPI_Receita Total AA = 
CALCULATE(
    [KPI_Receita Total],
    DATEADD(dCalendario[Data], -1, YEAR)
)

// 7. Variação Receita (YOY %)
KPI_Var Receita YOY = 
DIVIDE(
    [KPI_Receita Total] - [KPI_Receita Total AA],
    [KPI_Receita Total AA],
    0
)

// 8. Lucro Bruto - Ano Anterior
KPI_Lucro Bruto AA = 
CALCULATE(
    [KPI_Lucro Bruto],
    DATEADD(dCalendario[Data], -1, YEAR)
)

// 9. Variação Lucro (YOY %)
KPI_Var Lucro YOY = 
DIVIDE(
    [KPI_Lucro Bruto] - [KPI_Lucro Bruto AA],
    [KPI_Lucro Bruto AA],
    0
)
```

### Indicadores de Performance Operacional

```dax
// ══════════════════════════════════════════════════════════════
// INDICADORES OPERACIONAIS
// ══════════════════════════════════════════════════════════════

// 10. Taxa de Utilização de Frota
KPI_Utilizacao Frota = 
DIVIDE(
    [MAN_Peso Taxado],
    SUM(fManifestos[metadata.mft_vie_weight_capacity]), // Capacidade
    0
)

// 11. Eficiência Logística (Entregas no Prazo / Total)
KPI_Eficiencia Logistica = 
DIVIDE(
    [FRE_Finalizados Qtde],
    [FRE_Fretes Qtde],
    0
)

// 12. Produtividade Comercial (Conversão Cotações)
KPI_Produtividade Comercial = 
[COT_Taxa Conversao Valor]

// 13. Pontualidade Geral
KPI_Pontualidade = 
AVERAGEX(
    FILTER(
        SUMMARIZE(
            fColetas,
            fColetas[sequence_code],
            "NoP razo", IF(fColetas[finish_date] <= fColetas[service_date], 1, 0)
        ),
        [NoPrazo] = 1
    ),
    [NoPrazo]
)
```

---

## 🎨 FORMATAÇÕES

### Formatos de Medidas

```dax
// ══════════════════════════════════════════════════════════════
// FORMATAÇÕES PADRÃO
// ══════════════════════════════════════════════════════════════

// MOEDA (R$)
// Formato: "R$ "#,0.00

// MOEDA SEM CENTAVOS (R$)
// Formato: "R$ "#,0

// PERCENTUAL (%)
// Formato: 0.00%

// NÚMERO INTEIRO
// Formato: #,0

// NÚMERO DECIMAL
// Formato: #,0.00

// MILHARES (K)
// Formato: #,0"K"

// MILHÕES (M)
// Formato: #,0.0"M"
```

### Exemplos de Aplicação

```dax
// Aplicar formato moeda nas medidas de valor
COT_Realizadas Valor      → "R$ "#,0.00
FRE_Valor Total           → "R$ "#,0.00
MAN_Custo Total           → "R$ "#,0.00

// Aplicar formato percentual nas taxas
COT_Taxa Conversao Valor  → 0.00%
FRE_Taxa Conclusao        → 0.00%
MAN_Rentabilidade         → 0.00%

// Aplicar formato inteiro nas contagens
COT_Realizadas Qtde       → #,0
FRE_Fretes Qtde           → #,0
MAN_Manifestos Qtde       → #,0

// Aplicar formato decimal nos pesos
FRE_Peso Taxado           → #,0.00
COL_Peso Taxado           → #,0.00
```

---

## 🔢 COLUNAS CALCULADAS

### Tabela de Cotações

```dax
// 1. Status da Cotação (Derivado)
Status_Cotacao = 
IF(
    fCotacoes[fit_fhe_cte_issued_at] <> BLANK(),
    "CONVERTIDA",
    "PENDENTE"
)

// 2. Tempo até Conversão (Dias)
Dias_Ate_Conversao = 
DATEDIFF(
    fCotacoes[requested_at],
    fCotacoes[fit_fhe_cte_issued_at],
    DAY
)

// 3. Rota Completa
Rota = 
fCotacoes[origin_city] & " (" & fCotacoes[origin_state] & ") → " & 
fCotacoes[destination_city] & " (" & fCotacoes[destination_state] & ")"

// 4. Valor por Kg
Valor_por_Kg = 
DIVIDE(
    fCotacoes[total_value],
    fCotacoes[taxed_weight],
    0
)
```

### Tabela de Fretes

```dax
// 1. Status Simplificado
Status_Simples = 
SWITCH(
    fFretes[status],
    "finished", "Concluído",
    "pending", "Pendente",
    "delivering", "Em Entrega",
    "cancelled", "Cancelado",
    "Outro"
)

// 2. Rota Completa
Rota = 
fFretes[origem_cidade] & " (" & fFretes[origem_uf] & ") → " & 
fFretes[destino_cidade] & " (" & fFretes[destino_uf] & ")"

// 3. Modal Traduzido
Modal_PT = 
SWITCH(
    fFretes[modal],
    "rodo", "Rodoviário",
    "aereo", "Aéreo",
    "maritimo", "Marítimo",
    "Outro"
)

// 4. Tipo de Frete Traduzido
Tipo_Frete_PT = 
SWITCH(
    fFretes[tipo_frete],
    "Freight::Normal", "Normal",
    "Freight::Redelivery", "Reentrega",
    "Freight::Return", "Devolução",
    "Outro"
)
```

### Tabela de Manifestos

```dax
// 1. Status Traduzido
Status_PT = 
SWITCH(
    fManifestos[status],
    "in_transit", "Em Trânsito",
    "closed", "Fechado",
    "finished", "Finalizado",
    "cancelled", "Cancelado",
    "Outro"
)

// 2. Rentabilidade por Manifesto
Rentabilidade = 
DIVIDE(
    fManifestos[manifest_freights_total] - fManifestos[total_cost],
    fManifestos[manifest_freights_total],
    0
)

// 3. Custo por KM
Custo_por_KM = 
DIVIDE(
    fManifestos[total_cost],
    fManifestos[traveled_km],
    0
)

// 4. Tipo de Veículo Simplificado
Tipo_Veiculo_Simples = 
SWITCH(
    TRUE(),
    CONTAINSSTRING(fManifestos[vehicle_type], "Van"), "Van",
    CONTAINSSTRING(fManifestos[vehicle_type], "3/4"), "3/4",
    CONTAINSSTRING(fManifestos[vehicle_type], "Toco"), "Toco",
    CONTAINSSTRING(fManifestos[vehicle_type], "Truck"), "Truck",
    CONTAINSSTRING(fManifestos[vehicle_type], "Carreta"), "Carreta",
    "Outro"
)
```

### Tabela de Coletas

```dax
// 1. Status Traduzido
Status_PT = 
SWITCH(
    fColetas[status],
    "finished", "Finalizada",
    "pending", "Pendente",
    "created", "Criada",
    "manifested", "Manifestada",
    "in_transit", "Em Trânsito",
    "cancelled", "Cancelada",
    "Outro"
)

// 2. Tempo até Coleta (Horas)
Tempo_Ate_Coleta_Horas = 
DATEDIFF(
    fColetas[request_date],
    fColetas[finish_date],
    HOUR
)

// 3. No Prazo (Sim/Não)
No_Prazo = 
IF(
    fColetas[finish_date] <= fColetas[service_date],
    "Sim",
    "Não"
)

// 4. Atraso (Dias)
Atraso_Dias = 
IF(
    fColetas[finish_date] > fColetas[service_date],
    DATEDIFF(
        fColetas[service_date],
        fColetas[finish_date],
        DAY
    ),
    0
)
```

---

## 📈 TEXTO DINÂMICO (DAX para Títulos de Visuais)

### Títulos Dinâmicos

```dax
// 1. Título Período Selecionado
Titulo_Periodo = 
VAR _MinData = MIN(dCalendario[Data])
VAR _MaxData = MAX(dCalendario[Data])
VAR _MesUnico = DISTINCTCOUNT(dCalendario[MesNumero])
RETURN
    IF(
        _MesUnico = 1,
        "Mês de " & FORMAT(_MinData, "MMMM/YYYY"),
        FORMAT(_MinData, "DD/MM/YYYY") & " a " & FORMAT(_MaxData, "DD/MM/YYYY")
    )

// 2. Título Comparativo YOY
Titulo_Comparativo = 
VAR _AnoAtual = MAX(dCalendario[Ano])
VAR _AnoAnterior = _AnoAtual - 1
RETURN
    "Comparativo: " & _AnoAtual & " vs " & _AnoAnterior

// 3. Título com Filtro de Filial
Titulo_Filial = 
VAR _Filial = SELECTEDVALUE(fFretes[filial], "Todas as Filiais")
RETURN
    "Análise - " & _Filial

// 4. Título Dinâmico Cotações
Titulo_Cotacoes = 
"Cotações de Frete - " & 
SELECTEDVALUE(dCalendario[MesAbreviado], "Todos") & " " & 
SELECTEDVALUE(dCalendario[Ano], "")
```

---

## 🎯 MEDIDAS DE APOIO (Utilitárias)

### Medidas de Suporte

```dax
// 1. Data Selecionada (Auxiliar)
_Data Selecionada = 
MAX(dCalendario[Data])

// 2. Primeiro Dia do Mês
_Primeiro Dia Mes = 
DATE(YEAR([_Data Selecionada]), MONTH([_Data Selecionada]), 1)

// 3. Último Dia do Mês
_Ultimo Dia Mes = 
EOMONTH([_Data Selecionada], 0)

// 4. Quantidade de Dias no Mês
_Dias no Mes = 
DAY([_Ultimo Dia Mes])

// 5. Quantidade de Dias Úteis no Mês (Aproximado)
_Dias Uteis Mes = 
CALCULATE(
    COUNTROWS(dCalendario),
    dCalendario[IsFimDeSemana] = "Não",
    dCalendario[Data] >= [_Primeiro Dia Mes],
    dCalendario[Data] <= [_Ultimo Dia Mes]
)

// 6. Filtros Aplicados (Debug)
_Filtros Aplicados = 
VAR _TabelasFiltradas = 
    CONCATENATEX(
        FILTER(
            {
                ("Cotações", ISFILTERED(fCotacoes)),
                ("Fretes", ISFILTERED(fFretes)),
                ("Coletas", ISFILTERED(fColetas)),
                ("Manifestos", ISFILTERED(fManifestos))
            },
            [Value2] = TRUE
        ),
        [Value1],
        ", "
    )
RETURN
    IF(
        _TabelasFiltradas = BLANK(),
        "Nenhum filtro aplicado",
        "Filtrado: " & _TabelasFiltradas
    )
```

---

## 🔍 MEDIDAS DE RANKING E TOP N

### Rankings

```dax
// 1. Ranking Clientes por Valor (Cotações)
COT_Rank Cliente Valor = 
RANKX(
    ALL(fCotacoes[customer_name]),
    [COT_Realizadas Valor],
    ,
    DESC,
    DENSE
)

// 2. Ranking Filiais por Faturamento
FRE_Rank Filial Faturamento = 
RANKX(
    ALL(fFretes[filial]),
    [FRE_Valor Total],
    ,
    DESC,
    DENSE
)

// 3. Top 10 Clientes (Nome)
COT_Top10 Clientes = 
CALCULATE(
    CONCATENATEX(
        TOPN(
            10,
            VALUES(fCotacoes[customer_name]),
            [COT_Realizadas Valor],
            DESC
        ),
        fCotacoes[customer_name],
        ", "
    )
)

// 4. Top 5 Rotas (Texto)
FRE_Top5 Rotas = 
CALCULATE(
    CONCATENATEX(
        TOPN(
            5,
            VALUES(fFretes[Rota]), // Coluna calculada
            [FRE_Fretes Qtde],
            DESC
        ),
        fFretes[Rota],
        UNICHAR(10) // Quebra de linha
    )
)
```

---

## 🎨 MEDIDAS DE FORMATAÇÃO CONDICIONAL

### Cores Dinâmicas

```dax
// 1. Cor Status Cotação
COT_Cor Status = 
VAR _Status = SELECTEDVALUE(fCotacoes[Status_Cotacao])
RETURN
    SWITCH(
        _Status,
        "CONVERTIDA", "#28A745", // Verde
        "PENDENTE", "#FFC107",   // Amarelo
        "#6C757D"                // Cinza
    )

// 2. Cor Rentabilidade Manifesto
MAN_Cor Rentabilidade = 
VAR _Rent = [MAN_Rentabilidade]
RETURN
    SWITCH(
        TRUE(),
        _Rent >= 0.20, "#28A745", // Verde (≥ 20%)
        _Rent >= 0.10, "#FFC107", // Amarelo (10% a 19%)
        "#DC3545"                 // Vermelho (< 10%)
    )

// 3. Ícone Variação YOY
KPI_Icone Var YOY = 
VAR _Var = [KPI_Var Receita YOY]
RETURN
    IF(
        _Var > 0,
        UNICHAR(9650), // Triângulo para cima ▲
        UNICHAR(9660)  // Triângulo para baixo ▼
    )

// 4. Cor Pontualidade
COL_Cor Pontualidade = 
VAR _Pont = [COL_Taxa Pontualidade]
RETURN
    SWITCH(
        TRUE(),
        _Pont >= 0.95, "#28A745", // Verde (≥ 95%)
        _Pont >= 0.85, "#FFC107", // Amarelo (85% a 94%)
        "#DC3545"                 // Vermelho (< 85%)
    )
```

---

## ⚙️ CONFIGURAÇÕES RECOMENDADAS

### Conexões SQL Server

```powerquery
// Conexão para Cotações
let
    Source = Sql.Database("seu_servidor", "esl_cloud"),
    dbo_cotacoes = Source{[Schema="dbo",Item="cotacoes"]}[Data]
in
    dbo_cotacoes

// Conexão para Fretes
let
    Source = Sql.Database("seu_servidor", "esl_cloud"),
    dbo_fretes = Source{[Schema="dbo",Item="fretes"]}[Data]
in
    dbo_fretes

// Conexão para Coletas
let
    Source = Sql.Database("seu_servidor", "esl_cloud"),
    dbo_coletas = Source{[Schema="dbo",Item="coletas"]}[Data]
in
    dbo_coletas

// Conexão para Manifestos
let
    Source = Sql.Database("seu_servidor", "esl_cloud"),
    dbo_manifestos = Source{[Schema="dbo",Item="manifestos"]}[Data]
in
    dbo_manifestos

// Conexão para Contas a Pagar
let
    Source = Sql.Database("seu_servidor", "esl_cloud"),
    dbo_contas_a_pagar = Source{[Schema="dbo",Item="contas_a_pagar"]}[Data]
in
    dbo_contas_a_pagar

// Conexão para Faturas por Cliente
let
    Source = Sql.Database("seu_servidor", "esl_cloud"),
    dbo_faturas = Source{[Schema="dbo",Item="faturas_por_cliente"]}[Data]
in
    dbo_faturas
```

### Atualização Automática

```
1. Configurar Refresh Schedule no Power BI Service
2. Intervalo recomendado: 1x ao dia (madrugada)
3. Considerar incremental refresh para tabelas grandes
4. Monitorar falhas de atualização
```

---

## 📝 NOTAS FINAIS

### Próximos Passos

1. **Criar as conexões SQL** para cada tabela fato
2. **Implementar a tabela dCalendario** com todas as colunas calculadas
3. **Criar os relacionamentos** entre as tabelas
4. **Adicionar as medidas DAX** na tabela "Medidas"
5. **Criar as colunas calculadas** nas tabelas fato
6. **Configurar os formatos** das medidas
7. **Testar os relacionamentos** e filtros
8. **Criar os visuais** dos dashboards

### Dicas Importantes

- ✅ Use **DIVIDE()** ao invés de "/" para evitar divisão por zero
- ✅ Use **ISBLANK()** para tratar valores nulos
- ✅ Use **SELECTEDVALUE()** ao invés de **VALUES()** quando espera único valor
- ✅ Use **CALCULATE()** para modificar contexto de filtro
- ✅ Use **VAR** para armazenar resultados intermediários
- ✅ Teste medidas com diferentes filtros aplicados
- ✅ Documente medidas complexas com comentários

### Performance

- 🚀 Evite usar **FILTER()** em tabelas grandes sem necessidade
- 🚀 Use **SUMMARIZE()** para agregar antes de calcular
- 🚀 Prefira **colunas calculadas** para cálculos fixos (não mudam com contexto)
- 🚀 Prefira **medidas** para cálculos dinâmicos (mudam com contexto)
- 🚀 Use **DirectQuery** apenas se necessário (geralmente **Import** é melhor)

---

**FIM DO GUIA COMPLETO**

---

*Documento gerado em: 27/11/2025*  
*Versão: 1.0*  
*Autor: Sistema de Análise ETL*