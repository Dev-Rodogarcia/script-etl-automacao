package br.com.extrator.db.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.db.entity.FaturaPorClienteEntity;

/**
 * Repository para persistência de Faturas por Cliente no banco de dados.
 * Implementa operações MERGE (UPSERT) e criação automática de tabela.
 * 
 * @author Sistema de Extração ESL Cloud
 * @version 1.0
 */
public class FaturaPorClienteRepository extends AbstractRepository<FaturaPorClienteEntity> {
    private static final Logger logger = LoggerFactory.getLogger(FaturaPorClienteRepository.class);

    private static final String NOME_TABELA = "faturas_por_cliente";

    /**
     * Script SQL para criação da tabela se não existir.
     */
    private static final String SQL_CREATE_TABLE = """
        IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'faturas_por_cliente')
        BEGIN
            CREATE TABLE faturas_por_cliente (
                unique_id NVARCHAR(100) PRIMARY KEY,
                valor_frete DECIMAL(18,2),
                valor_fatura DECIMAL(18,2),
                numero_cte BIGINT,
                chave_cte NVARCHAR(100),
                numero_nfse BIGINT,
                status_cte NVARCHAR(255),
                data_emissao_cte DATETIMEOFFSET,
                numero_fatura NVARCHAR(50),
                data_emissao_fatura DATE,
                data_vencimento_fatura DATE,
                data_baixa_fatura DATE,
                fit_ant_document NVARCHAR(50),
                fit_ant_issue_date DATE,
                fit_ant_value DECIMAL(18,2),
                filial NVARCHAR(255),
                tipo_frete NVARCHAR(100),
                classificacao NVARCHAR(100),
                estado NVARCHAR(50),
                pagador_nome NVARCHAR(255),
                pagador_documento NVARCHAR(50),
                remetente_nome NVARCHAR(255),
                destinatario_nome NVARCHAR(255),
                vendedor_nome NVARCHAR(255),
                notas_fiscais NVARCHAR(MAX),
                pedidos_cliente NVARCHAR(MAX),
                metadata NVARCHAR(MAX),
                data_extracao DATETIME2 DEFAULT GETDATE()
            );
            CREATE INDEX IX_fpc_vencimento ON faturas_por_cliente(data_vencimento_fatura);
            CREATE INDEX IX_fpc_pagador ON faturas_por_cliente(pagador_nome);
            CREATE INDEX IX_fpc_filial ON faturas_por_cliente(filial);
            CREATE INDEX IX_fpc_chave_cte ON faturas_por_cliente(chave_cte);
        END
        """;

    /**
     * SQL MERGE para INSERT ou UPDATE usando unique_id como chave.
     */
    private static final String SQL_MERGE = """
        MERGE INTO faturas_por_cliente AS target
        USING (
            SELECT
                ? AS unique_id,
                ? AS valor_frete,
                ? AS valor_fatura,
                ? AS numero_cte,
                ? AS chave_cte,
                ? AS numero_nfse,
                ? AS status_cte,
                ? AS data_emissao_cte,
                ? AS numero_fatura,
                ? AS data_emissao_fatura,
                ? AS data_vencimento_fatura,
                ? AS data_baixa_fatura,
                ? AS fit_ant_document,
                ? AS fit_ant_issue_date,
                ? AS fit_ant_value,
                ? AS filial,
                ? AS tipo_frete,
                ? AS classificacao,
                ? AS estado,
                ? AS pagador_nome,
                ? AS pagador_documento,
                ? AS remetente_nome,
                ? AS destinatario_nome,
                ? AS vendedor_nome,
                ? AS notas_fiscais,
                ? AS pedidos_cliente,
                ? AS metadata,
                GETDATE() AS data_extracao
        ) AS source
        ON target.unique_id = source.unique_id
        WHEN MATCHED THEN
            UPDATE SET
                valor_frete = source.valor_frete,
                valor_fatura = source.valor_fatura,
                numero_cte = source.numero_cte,
                chave_cte = source.chave_cte,
                numero_nfse = source.numero_nfse,
                status_cte = source.status_cte,
                data_emissao_cte = source.data_emissao_cte,
                numero_fatura = source.numero_fatura,
                data_emissao_fatura = source.data_emissao_fatura,
                data_vencimento_fatura = source.data_vencimento_fatura,
                data_baixa_fatura = source.data_baixa_fatura,
                fit_ant_document = source.fit_ant_document,
                fit_ant_issue_date = source.fit_ant_issue_date,
                fit_ant_value = source.fit_ant_value,
                filial = source.filial,
                tipo_frete = source.tipo_frete,
                classificacao = source.classificacao,
                estado = source.estado,
                pagador_nome = source.pagador_nome,
                pagador_documento = source.pagador_documento,
                remetente_nome = source.remetente_nome,
                destinatario_nome = source.destinatario_nome,
                vendedor_nome = source.vendedor_nome,
                notas_fiscais = source.notas_fiscais,
                pedidos_cliente = source.pedidos_cliente,
                metadata = source.metadata,
                data_extracao = source.data_extracao
        WHEN NOT MATCHED THEN
            INSERT (unique_id, valor_frete, valor_fatura, numero_cte, chave_cte, numero_nfse, status_cte, data_emissao_cte, numero_fatura, data_emissao_fatura, data_vencimento_fatura, data_baixa_fatura, fit_ant_document, fit_ant_issue_date, fit_ant_value, filial, tipo_frete, classificacao, estado, pagador_nome, pagador_documento, remetente_nome, destinatario_nome, vendedor_nome, notas_fiscais, pedidos_cliente, metadata, data_extracao)
            VALUES (source.unique_id, source.valor_frete, source.valor_fatura, source.numero_cte, source.chave_cte, source.numero_nfse, source.status_cte, source.data_emissao_cte, source.numero_fatura, source.data_emissao_fatura, source.data_vencimento_fatura, source.data_baixa_fatura, source.fit_ant_document, source.fit_ant_issue_date, source.fit_ant_value, source.filial, source.tipo_frete, source.classificacao, source.estado, source.pagador_nome, source.pagador_documento, source.remetente_nome, source.destinatario_nome, source.vendedor_nome, source.notas_fiscais, source.pedidos_cliente, source.metadata, source.data_extracao);
        """;

    @Override
    protected String getNomeTabela() {
        return NOME_TABELA;
    }

    @Override
    protected void criarTabelaSeNaoExistir(final Connection conexao) throws SQLException {
        executarDDL(conexao, SQL_CREATE_TABLE);
        logger.info("Verificação/criação da tabela {} concluída", NOME_TABELA);
        criarViewPowerBISeNaoExistir(conexao);
    }

    private void criarViewPowerBISeNaoExistir(final Connection conexao) throws SQLException {
        final String sqlView = """
            CREATE OR ALTER VIEW dbo.vw_faturas_por_cliente_powerbi AS
            SELECT
                unique_id AS [ID Único],
                filial AS [Filial],
                numero_cte AS [CT-e / Número],
                chave_cte AS [CT-e / Chave],
                data_emissao_cte AS [CT-e / Data Emissão],
                valor_frete AS [CT-e / Valor Frete],
                status_cte AS [CT-e / Status],
                tipo_frete AS [Tipo de Frete],
                classificacao AS [Classificação],
                pagador_nome AS [Pagador / Nome],
                pagador_documento AS [Pagador / CNPJ],
                remetente_nome AS [Remetente],
                destinatario_nome AS [Destinatário],
                vendedor_nome AS [Vendedor],
                CASE WHEN fit_ant_document IS NOT NULL THEN 'Faturado' ELSE 'Aguardando Faturamento' END AS [Status do Processo],
                fit_ant_document AS [Fatura / Número Doc],
                fit_ant_issue_date AS [Fatura / Data Emissão],
                fit_ant_value AS [Fatura / Valor Total],
                data_extracao AS [Data da Última Atualização]
            FROM dbo.faturas_por_cliente;
        """;
        executarDDL(conexao, sqlView);
    }

    @Override
    protected int executarMerge(final Connection conexao, final FaturaPorClienteEntity entity) throws SQLException {
        validarEntidade(entity);

        try (PreparedStatement pstmt = conexao.prepareStatement(SQL_MERGE)) {
            int idx = 1;

            pstmt.setString(idx++, entity.getUniqueId());
            setBigDecimalParameter(pstmt, idx++, entity.getValorFrete());
            setBigDecimalParameter(pstmt, idx++, entity.getValorFatura());
            setLongParameter(pstmt, idx++, entity.getNumeroCte());
            pstmt.setString(idx++, entity.getChaveCte());
            setLongParameter(pstmt, idx++, entity.getNumeroNfse());
            pstmt.setString(idx++, entity.getStatusCte());
            if (entity.getDataEmissaoCte() != null) {
                pstmt.setObject(idx++, entity.getDataEmissaoCte(), java.sql.Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                pstmt.setNull(idx++, java.sql.Types.TIMESTAMP_WITH_TIMEZONE);
            }
            pstmt.setString(idx++, entity.getNumeroFatura());
            setDateParameter(pstmt, idx++, entity.getDataEmissaoFatura());
            setDateParameter(pstmt, idx++, entity.getDataVencimentoFatura());
            setDateParameter(pstmt, idx++, entity.getDataBaixaFatura());
            pstmt.setString(idx++, entity.getFitAntDocument());
            setDateParameter(pstmt, idx++, entity.getFitAntIssueDate());
            setBigDecimalParameter(pstmt, idx++, entity.getFitAntValue());
            pstmt.setString(idx++, entity.getFilial());
            pstmt.setString(idx++, entity.getTipoFrete());
            pstmt.setString(idx++, entity.getClassificacao());
            pstmt.setString(idx++, entity.getEstado());
            pstmt.setString(idx++, entity.getPagadorNome());
            pstmt.setString(idx++, entity.getPagadorDocumento());
            pstmt.setString(idx++, entity.getRemetenteNome());
            pstmt.setString(idx++, entity.getDestinatarioNome());
            pstmt.setString(idx++, entity.getVendedorNome());
            pstmt.setString(idx++, entity.getNotasFiscais());
            pstmt.setString(idx++, entity.getPedidosCliente());
            pstmt.setString(idx++, entity.getMetadata());

            final int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                logger.warn("MERGE não afetou nenhuma linha para unique_id: {}", entity.getUniqueId());
            }
            return rowsAffected;
        }
    }

    

    /**
     * Valida campos obrigatórios da entidade antes de salvar.
     */
    private void validarEntidade(final FaturaPorClienteEntity entity) {
        if (entity.getUniqueId() == null || entity.getUniqueId().trim().isEmpty()) {
            throw new IllegalArgumentException("unique_id não pode ser null ou vazio");
        }
        // Validações adicionais podem ser adicionadas aqui
    }
}
