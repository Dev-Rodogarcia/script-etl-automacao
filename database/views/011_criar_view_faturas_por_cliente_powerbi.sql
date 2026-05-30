IF OBJECT_ID(N'dbo.faturas_por_cliente', N'U') IS NOT NULL
   AND COL_LENGTH('dbo.faturas_por_cliente', 'cliente_cnpj') IS NULL
BEGIN
    ALTER TABLE dbo.faturas_por_cliente ADD cliente_cnpj NVARCHAR(14) NULL;
END;
GO

CREATE OR ALTER VIEW dbo.vw_faturas_por_cliente_powerbi AS
SELECT

    CAST(datas.data_emissao_cte AS TIME(0)) AS [Hora (Solicitacao)],
    fpc.unique_id AS [ID Único],
    fpc.filial AS [Filial],
    fpc.estado AS [Estado],
    fpc.numero_cte AS [CT-e/Número],
    CASE 
        WHEN fpc.numero_cte IS NOT NULL THEN CONVERT(NVARCHAR(50), fpc.numero_cte)
        WHEN fpc.numero_nfse IS NOT NULL THEN CONVERT(NVARCHAR(50), fpc.numero_nfse)
        ELSE NULL
    END AS [Número do Documento],
    fpc.chave_cte AS [CT-e/Chave],
    datas.data_emissao_cte AS [CT-e/Data de emissão],
    fpc.valor_frete AS [Frete/Valor dos CT-es],
    fpc.third_party_ctes_value AS [Terceiros/Valor CT-es],
    fpc.status_cte AS [CT-e/Status],
    fpc.status_cte_result AS [CT-e/Resultado],
    fpc.tipo_frete AS [Tipo],
    fpc.classificacao AS [Classificação],
    fpc.pagador_nome AS [Pagador do frete/Nome],
    fpc.pagador_documento AS [Pagador do frete/Documento],
    COALESCE(fpc.cliente_cnpj, documentos.pagador_cnpj) AS [Cliente/CNPJ],
    fpc.remetente_nome AS [Remetente/Nome],
    fpc.remetente_documento AS [Remetente/Documento],
    fpc.destinatario_nome AS [Destinatário/Nome],
    fpc.destinatario_documento AS [Destinatário/Documento],
    fpc.vendedor_nome AS [Vendedor/Nome],
    fpc.numero_nfse AS [NFS-e/Número],
    fpc.serie_nfse AS [NFS-e/Série],
    fpc.numero_nfse AS [fit_nse_number],
    fpc.numero_nfse AS [N° NFS-e],
    CAST(NULL AS NVARCHAR(50)) AS [Carteira/Descrição],
    CAST(NULL AS NVARCHAR(MAX)) AS [Instrução Customizada],
    CASE WHEN fpc.fit_ant_document IS NOT NULL THEN 'Faturado' ELSE 'Aguardando Faturamento' END AS [Status do Processo],
    fpc.fit_ant_document AS [Fatura/N° Documento],
    CONVERT(NVARCHAR(10), datas.fit_ant_issue_date, 23) AS [Fatura/Emissão],
    fpc.fit_ant_value AS [Fatura/Valor],
    fpc.valor_fatura AS [Fatura/Valor Total],
    fpc.numero_fatura AS [Fatura/Número],
    CONVERT(NVARCHAR(10), datas.data_emissao_fatura, 23) AS [Fatura/Emissão Fatura],
    CONVERT(NVARCHAR(10), datas.data_vencimento_fatura, 23) AS [Parcelas/Vencimento],
    CONVERT(NVARCHAR(10), datas.data_baixa_fatura, 23) AS [Fatura/Baixa],
    CONVERT(NVARCHAR(10), datas.fit_ant_ils_original_due_date, 23) AS [Fatura/Data Vencimento Original],
    fpc.notas_fiscais AS [Notas Fiscais],
    fpc.pedidos_cliente AS [Pedidos/Cliente],
    fpc.metadata AS [Metadata],
    fpc.data_extracao AS [Data da Última Atualização]
FROM dbo.faturas_por_cliente fpc
OUTER APPLY (
    SELECT
        TRY_CONVERT(DATETIMEOFFSET, fpc.data_emissao_cte) AS data_emissao_cte,
        TRY_CONVERT(DATE, fpc.fit_ant_issue_date) AS fit_ant_issue_date,
        TRY_CONVERT(DATE, fpc.data_emissao_fatura) AS data_emissao_fatura,
        TRY_CONVERT(DATE, fpc.data_vencimento_fatura) AS data_vencimento_fatura,
        TRY_CONVERT(DATE, fpc.data_baixa_fatura) AS data_baixa_fatura,
        TRY_CONVERT(DATE, fpc.fit_ant_ils_original_due_date) AS fit_ant_ils_original_due_date
) datas
OUTER APPLY (
    SELECT
        CASE
            WHEN documento_limpo NOT LIKE '%[^0-9]%'
             AND LEN(documento_limpo) = 14
            THEN documento_limpo
            ELSE NULL
        END AS pagador_cnpj
    FROM (
        SELECT REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
            LTRIM(RTRIM(COALESCE(CONVERT(NVARCHAR(50), fpc.pagador_documento), N''))),
            N'.', N''), N'-', N''), N'/', N''), N' ', N''), CHAR(9), N''), CHAR(160), N'') AS documento_limpo
    ) limpeza
) documentos
WHERE COALESCE(fpc.excluido_na_origem, 0) = 0;
GO
