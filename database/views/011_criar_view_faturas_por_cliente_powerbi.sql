-- ============================================
-- Script de criação da view 'vw_faturas_por_cliente_powerbi'
-- Execute este script UMA VEZ antes de colocar o sistema em produção
-- ============================================

CREATE OR ALTER VIEW dbo.vw_faturas_por_cliente_powerbi AS
SELECT
    unique_id AS [ID Único],
    filial AS [Filial],
    estado AS [Estado],
    numero_cte AS [CT-e/Número],
    CASE 
        WHEN numero_cte IS NOT NULL THEN CONVERT(NVARCHAR(50), numero_cte)
        WHEN numero_nfse IS NOT NULL THEN CONVERT(NVARCHAR(50), numero_nfse)
        ELSE NULL
    END AS [Número do Documento],
    chave_cte AS [CT-e/Chave],
    data_emissao_cte AS [CT-e/Data de emissão],
    valor_frete AS [Frete/Valor dos CT-es],
    third_party_ctes_value AS [Terceiros/Valor CT-es],
    status_cte AS [CT-e/Status],
    status_cte_result AS [CT-e/Resultado],
    tipo_frete AS [Tipo],
    classificacao AS [Classificação],
    pagador_nome AS [Pagador do frete/Nome],
    pagador_documento AS [Pagador do frete/Documento],
    remetente_nome AS [Remetente/Nome],
    remetente_documento AS [Remetente/Documento],
    destinatario_nome AS [Destinatário/Nome],
    destinatario_documento AS [Destinatário/Documento],
    vendedor_nome AS [Vendedor/Nome],
    numero_nfse AS [NFS-e/Número],
    serie_nfse AS [NFS-e/Série],
    numero_nfse AS [fit_nse_number],
    CASE WHEN fit_ant_document IS NOT NULL THEN 'Faturado' ELSE 'Aguardando Faturamento' END AS [Status do Processo],
    fit_ant_document AS [Fatura/N° Documento],
    fit_ant_issue_date AS [Fatura/Emissão],
    fit_ant_value AS [Fatura/Valor],
    valor_fatura AS [Fatura/Valor Total],
    numero_fatura AS [Fatura/Número],
    data_emissao_fatura AS [Fatura/Emissão Fatura],
    data_vencimento_fatura AS [Parcelas/Vencimento],
    data_baixa_fatura AS [Fatura/Baixa],
    fit_ant_ils_original_due_date AS [Fatura/Data Vencimento Original],
    notas_fiscais AS [Notas Fiscais],
    pedidos_cliente AS [Pedidos/Cliente],
    metadata AS [Metadata],
    data_extracao AS [Data da Última Atualização]
FROM dbo.faturas_por_cliente;
GO

PRINT 'View vw_faturas_por_cliente_powerbi criada/atualizada com sucesso!';
GO
