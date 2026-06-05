# contas_a_pagar

## Objetivo de negocio

Controlar titulos de despesas e pagamentos do Data Export para analise financeira, competencia, fornecedor, filial e centro de custo.

## Chaves e deduplicacao

- Grao: 1 linha por titulo de conta a pagar.
- Chave primaria: `sequence_code`.
- Deduplicacao: `ant_ils_sequence_code` e convertido para `sequence_code` e usado no upsert idempotente.

## De/Para JSON API -> SQL

| JSON API | Coluna SQL |
|---|---|
| `ant_ils_sequence_code` | `sequence_code` |
| `document` | `document_number` |
| `issue_date` | `issue_date` |
| `type` | `tipo_lancamento` |
| `value` | `valor_original` |
| `interest_value` | `valor_juros` |
| `discount_value` | `valor_desconto` |
| `value_to_pay` | `valor_a_pagar` |
| `paid_value` | `valor_pago` |
| `paid` | `status_pagamento` |
| `competence_month` | `mes_competencia` |
| `competence_year` | `ano_competencia` |
| `created_at` | `data_criacao` |
| `ant_ils_atn_liquidation_date` | `data_liquidacao` |
| `ant_ils_atn_transaction_date` | `data_transacao` |
| `ant_rir_name` | `nome_fornecedor` |
| `ant_crn_psn_nickname` | `nome_filial` |
| `ant_ces_acr_name` | `nome_centro_custo` |
| `ant_ces_value` | `valor_centro_custo` |
| `ant_ils_pas_ant_classification` | `classificacao_contabil` |
| `ant_ils_pas_ant_name` | `descricao_contabil` |
| `ant_ils_pas_value` | `valor_contabil` |
| `ant_aln_name` | `area_lancamento` |
| `ant_ils_comments` | `observacoes` |
| `ant_ils_expense_description` | `descricao_despesa` |
| `ant_uer_name` | `nome_usuario` |
| `ant_ils_atn_reconciled` | `reconciliado` |
| payload completo | `metadata` |
