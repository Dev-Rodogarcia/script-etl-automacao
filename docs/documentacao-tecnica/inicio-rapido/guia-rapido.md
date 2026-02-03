# ⚡ Guia Rápido - v2.0

## 🚀 Começando em 5 Minutos

### 1️⃣ Compilar (1 min)
```bash
# Windows
05-compilar_projeto.bat

# Aguarde: "BUILD SUCCESS"
```

### 2️⃣ Executar (2 min)
```bash
# Extração completa
01-executar_extracao_completa.bat

# Aguarde: "Extração concluída com sucesso"
```

### 3️⃣ Validar (2 min)
```sql
-- Abra SQL Server Management Studio
-- Execute:

SELECT TOP 10
    id,
    document_number,
    filial,           -- NOVO ✨
    cnpj_filial,      -- NOVO ✨
    conta_contabil,   -- NOVO ✨
    centro_custo,     -- NOVO ✨
    status,           -- NOVO ✨
    observacoes       -- NOVO ✨
FROM faturas_a_pagar
ORDER BY data_extracao DESC;
```

**✅ Pronto!** Se você vê dados nas colunas novas, está funcionando!

---

## 🎯 O Que Mudou?

### Antes (v1.0)
```
Fatura #12345
├── Valor: R$ 1.500,00
├── Vencimento: 30/11/2025
└── Fornecedor: XYZ Ltda
```

### Agora (v2.0)
```
Fatura #12345
├── Valor: R$ 1.500,00
├── Vencimento: 30/11/2025
├── Fornecedor: XYZ Ltda (98.765.432/0001-10)
├── Filial: Filial SP (12.345.678/0001-90)      ✨ NOVO
├── Status: Pendente                             ✨ NOVO
├── Conta Contábil: Despesas Operacionais        ✨ NOVO
├── Centro de Custo: Centro A, Centro B          ✨ NOVO
└── Observações: Pagamento urgente               ✨ NOVO
```

---

## 📊 Consultas Úteis

### Dashboard Rápido
```sql
SELECT 
    COUNT(*) as total,
    SUM(total_value) as valor_total,
    COUNT(CASE WHEN status = 'Vencido' THEN 1 END) as vencidas,
    COUNT(DISTINCT filial) as filiais
FROM faturas_a_pagar
WHERE data_extracao >= CAST(GETDATE() AS DATE);
```

### Faturas Vencidas
```sql
SELECT 
    document_number,
    receiver_name,
    total_value,
    due_date,
    DATEDIFF(DAY, due_date, GETDATE()) as dias_atraso
FROM faturas_a_pagar
WHERE status = 'Vencido'
ORDER BY dias_atraso DESC;
```

### Por Filial
```sql
SELECT 
    filial,
    COUNT(*) as qtd,
    SUM(total_value) as valor
FROM faturas_a_pagar
WHERE data_extracao >= CAST(GETDATE() AS DATE)
GROUP BY filial
ORDER BY valor DESC;
```

---

## 🐛 Problemas Comuns

### ❌ Novos campos estão NULL
**Causa:** API não retornou os dados  
**Solução:** Normal para alguns campos. Verifique `header_metadata`

### ❌ Status sempre "Indefinido"
**Causa:** `due_date` está NULL  
**Solução:** Verifique se a API está retornando a data de vencimento

### ❌ Erro de compilação
**Causa:** JAVA_HOME não configurado  
**Solução:** 
```bash
java -version
# Se não funcionar, instale Java 17+
```

---

## 📚 Documentação Completa

| Documento | Quando Usar |
|-----------|-------------|
| `README_ATUALIZACAO_REST.md` | Visão geral completa |
| `CHECKLIST_VALIDACAO_CAMPOS.md` | Testes detalhados |
| `EXEMPLOS_USO_NOVOS_CAMPOS.md` | Consultas SQL avançadas |
| `DIAGRAMA_ESTRUTURA_ATUALIZADA.md` | Entender arquitetura |
| `SUMARIO_EXECUTIVO_v2.0.md` | Apresentação executiva |

---

## ✅ Checklist Rápido

- [ ] Compilou sem erros?
- [ ] Extração executada?
- [ ] Novos campos aparecem no banco?
- [ ] Status está sendo calculado?
- [ ] Dados fazem sentido?

**Tudo OK?** 🎉 Você está pronto para usar a v2.0!

---

## 🆘 Precisa de Ajuda?

1. **Logs:** Verifique `logs/` para erros
2. **Documentação:** Leia `docs/README_ATUALIZACAO_REST.md`
3. **Exemplos:** Consulte `docs/EXEMPLOS_USO_NOVOS_CAMPOS.md`

---

**Versão:** 2.0.0  
**Atualizado:** 04/11/2025

