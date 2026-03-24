CREATE OR ALTER VIEW dbo.vw_faturas_por_cliente_powerbi AS
SELECT

    CAST(fpc.data_emissao_cte AS TIME(0)) AS [Hora (Solicitacao)],
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
    fpc.data_emissao_cte AS [CT-e/Data de emissão],
    fpc.valor_frete AS [Frete/Valor dos CT-es],
    fpc.third_party_ctes_value AS [Terceiros/Valor CT-es],
    fpc.status_cte AS [CT-e/Status],
    fpc.status_cte_result AS [CT-e/Resultado],
    fpc.tipo_frete AS [Tipo],
    fpc.classificacao AS [Classificação],
    fpc.pagador_nome AS [Pagador do frete/Nome],
    fpc.pagador_documento AS [Pagador do frete/Documento],
    fpc.remetente_nome AS [Remetente/Nome],
    fpc.remetente_documento AS [Remetente/Documento],
    fpc.destinatario_nome AS [Destinatário/Nome],
    fpc.destinatario_documento AS [Destinatário/Documento],
    fpc.vendedor_nome AS [Vendedor/Nome],
    fpc.numero_nfse AS [NFS-e/Número],
    fpc.serie_nfse AS [NFS-e/Série],
    fpc.numero_nfse AS [fit_nse_number],
    fg.nfse_numero AS [N° NFS-e],
    fg.carteira_banco AS [Carteira/Descrição],
    fg.instrucao_boleto AS [Instrução Customizada],
    CASE WHEN fpc.fit_ant_document IS NOT NULL THEN 'Faturado' ELSE 'Aguardando Faturamento' END AS [Status do Processo],
    fpc.fit_ant_document AS [Fatura/N° Documento],
    fpc.fit_ant_issue_date AS [Fatura/Emissão],
    fpc.fit_ant_value AS [Fatura/Valor],
    fpc.valor_fatura AS [Fatura/Valor Total],
    fpc.numero_fatura AS [Fatura/Número],
    fpc.data_emissao_fatura AS [Fatura/Emissão Fatura],
    fpc.data_vencimento_fatura AS [Parcelas/Vencimento],
    fpc.data_baixa_fatura AS [Fatura/Baixa],
    fpc.fit_ant_ils_original_due_date AS [Fatura/Data Vencimento Original],
    fpc.notas_fiscais AS [Notas Fiscais],
    fpc.pedidos_cliente AS [Pedidos/Cliente],
    fpc.metadata AS [Metadata],
    fpc.data_extracao AS [Data da Última Atualização]
FROM dbo.faturas_por_cliente fpc
LEFT JOIN dbo.faturas_graphql fg ON fpc.fit_ant_document = fg.document;
GO
