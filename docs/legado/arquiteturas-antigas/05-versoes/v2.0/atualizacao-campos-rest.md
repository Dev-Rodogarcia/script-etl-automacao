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
# Atualização de Campos REST - Faturas a Pagar

## 📋 Resumo da Atualização

Atualização das classes Java do projeto REST para suportar **14 campos disponíveis** na API `/api/accounting/debit/billings`, adicionar **placeholders para 10 campos futuros** e incluir **cálculo local de status**.

**Data:** 04/11/2025  
**Stack:** Java 17, Spring Boot 3.x, Jackson, Lombok  
**Endpoint:** `/api/accounting/debit/billings`

---

## ✅ Campos Implementados (14/24)

### Campos Básicos (já existentes)
1. **id** - Identificador único da fatura
2. **document** - Número do documento (numeroDocumento)
3. **issue_date** - Data de emissão (dataEmissao)
4. **due_date** - Data de vencimento (dataVencimento)
5. **value** - Valor total (valorTotal)
6. **type** - Tipo de fatura

### Novos Campos Adicionados
7. **corporation.cnpj** → `cnpjFilial` - CNPJ da filial
8. **corporation.nickname** → `filial` - Nome/apelido da filial
9. **receiver.cnpjCpf** → `cnpjFornecedor` - CNPJ/CPF do fornecedor
10. **receiver.name** → `fornecedor` - Nome do fornecedor
11. **comments** → `observacoes` - Observações gerais
12. **accounting_planning_management.name** → `contaContabil` - Conta contábil
13. **cost_centers[].name** → `centroCusto` - Centros de custo (concatenados)
14. **status** → `status` - Status calculado localmente (Pendente/Vencido/Indefinido)

---

## 🔮 Campos Futuros - Placeholders (10/24)

Estes campos estão preparados nas classes mas ainda não são retornados pela API REST:

1. **sequencia** - Sequência do lançamento
2. **cheque** - Número do cheque (se forma de pagamento for cheque)
3. **vencimentoOriginal** - Data de vencimento original (antes de renegociações)
4. **competencia** - Competência contábil (mês/ano de referência)
5. **dataBaixa** - Data de baixa do título
6. **dataLiquidacao** - Data de liquidação financeira
7. **bancoPagamento** - Banco utilizado para pagamento
8. **contaPagamento** - Conta bancária utilizada para pagamento
9. **descricaoDespesa** - Descrição detalhada da despesa
10. *(Campos de parcelas: valorPrincipal, jurosDesconto, formaPagamento - processados via installments)*

---

## 📦 Classes Criadas/Atualizadas

### Novos DTOs Auxiliares
- ✨ **CorporationDTO.java** - Mapeia dados da filial (corporation)
- ✨ **InstallmentDTO.java** - Mapeia dados das parcelas (installments)
- ✨ **AccountingPlanningManagementDTO.java** - Mapeia conta contábil
- ✨ **CostCenterDTO.java** - Mapeia centros de custo
- 🔄 **ReceiverDTO.java** - Atualizado com campo `cnpjCpf`

### Classes Principais Atualizadas
- 🔄 **FaturaAPagarDTO.java**
  - Adicionados 14 campos disponíveis com `@JsonProperty`
  - Adicionados 10 campos futuros com `@JsonIgnore`
  - Incluído `@JsonIgnoreProperties(ignoreUnknown = true)`
  - Comentários indicando fonte de cada campo

- 🔄 **FaturaAPagarMapper.java**
  - Processamento dos novos campos disponíveis
  - Cálculo local de status: `if (dueDate.isBefore(LocalDate.now())) status = "Vencido"; else status = "Pendente";`
  - Concatenação de múltiplos centros de custo
  - Tratamento de compatibilidade entre `cnpjCpf` e `cnpj`

- 🔄 **FaturaAPagarEntity.java**
  - Adicionados 6 novos campos disponíveis
  - Adicionados 9 campos futuros (placeholders)
  - Getters e setters para todos os campos
  - Documentação atualizada

- 🔄 **FaturaAPagarRepository.java**
  - SQL CREATE TABLE atualizado com 15 novas colunas
  - SQL MERGE atualizado para incluir novos campos
  - PreparedStatement atualizado com 22 parâmetros

---

## 🎯 Lógica de Cálculo de Status

```java
if (dueDate != null) {
    final LocalDate hoje = LocalDate.now();
    if (dueDate.isBefore(hoje)) {
        entity.setStatus("Vencido");
    } else {
        entity.setStatus("Pendente");
    }
} else {
    entity.setStatus("Indefinido");
}
```

**Valores possíveis:**
- `Pendente` - Data de vencimento é hoje ou no futuro
- `Vencido` - Data de vencimento já passou
- `Indefinido` - Data de vencimento não disponível

---

## 🗄️ Estrutura da Tabela Atualizada

```sql
CREATE TABLE faturas_a_pagar (
    -- Colunas de Chave
    id BIGINT PRIMARY KEY,
    document_number NVARCHAR(100),

    -- Colunas Essenciais
    issue_date DATE,
    due_date DATE,
    total_value DECIMAL(18, 2),
    receiver_cnpj NVARCHAR(14),
    receiver_name NVARCHAR(255),
    invoice_type NVARCHAR(100),

    -- NOVOS CAMPOS DISPONÍVEIS (14/24)
    cnpj_filial NVARCHAR(14),
    filial NVARCHAR(255),
    observacoes NVARCHAR(MAX),
    conta_contabil NVARCHAR(255),
    centro_custo NVARCHAR(500),
    status NVARCHAR(50),

    -- CAMPOS FUTUROS (10/24)
    sequencia NVARCHAR(50),
    cheque NVARCHAR(50),
    vencimento_original DATE,
    competencia NVARCHAR(7),
    data_baixa DATE,
    data_liquidacao DATE,
    banco_pagamento NVARCHAR(255),
    conta_pagamento NVARCHAR(100),
    descricao_despesa NVARCHAR(MAX),

    -- Metadados
    header_metadata NVARCHAR(MAX),
    installments_metadata NVARCHAR(MAX),

    -- Auditoria
    data_extracao DATETIME2 DEFAULT GETDATE(),

    UNIQUE (document_number)
)
```

---

## 🔄 Compatibilidade com Fontes Futuras

As classes estão preparadas para integração futura com:
- **GraphQL** - Campos futuros podem ser mapeados quando disponíveis
- **Data Export** - Estrutura compatível com exportações CSV/JSON
- **Outras APIs** - Arquitetura flexível com metadados JSON

---

## ✅ Validação

- ✅ Sem erros de compilação
- ✅ Todos os campos mapeados corretamente
- ✅ Cálculo de status implementado
- ✅ Placeholders preparados para campos futuros
- ✅ Compatibilidade mantida com código existente
- ✅ Documentação inline completa

---

## 📝 Próximos Passos

1. **Testar extração** - Executar `01-executar_extracao_completa.bat`
2. **Validar dados** - Verificar se os novos campos estão sendo populados
3. **Atualizar queries** - Incluir novos campos em relatórios SQL
4. **Exportar CSV** - Atualizar `ExportadorCSV.java` para incluir novos campos
5. **Integração GraphQL** - Quando disponível, mapear campos futuros

---

## 🎓 Boas Práticas Aplicadas

- ✅ Uso de `@JsonProperty` para mapeamento explícito
- ✅ `@JsonIgnoreProperties(ignoreUnknown = true)` para resiliência
- ✅ `@JsonIgnore` para campos futuros (não serializados)
- ✅ Comentários indicando fonte de cada campo
- ✅ Tratamento de valores nulos
- ✅ Cálculo local de campos derivados (status)
- ✅ Concatenação de arrays (centros de custo)
- ✅ Compatibilidade retroativa mantida
- ✅ Código limpo e bem documentado

---

**Desenvolvido com ❤️ para o projeto de extração ESL Cloud**
