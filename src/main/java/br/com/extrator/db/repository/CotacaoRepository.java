package br.com.extrator.db.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.db.entity.CotacaoEntity;

/**
 * Repositório para operações de persistência da entidade CotacaoEntity.
 * Implementa a arquitetura de persistência híbrida: colunas-chave para
 * indexação
 * e uma coluna de metadados para resiliência e completude dos dados.
 * Utiliza operações MERGE (UPSERT) com a chave de negócio (sequenceCode).
 */
public class CotacaoRepository extends AbstractRepository<CotacaoEntity> {
    private static final Logger logger = LoggerFactory.getLogger(CotacaoRepository.class);
    private static final String NOME_TABELA = "cotacoes";
    private static boolean viewVerificada = false;

    @Override
    protected String getNomeTabela() {
        return NOME_TABELA;
    }

    /**
     * Cria a tabela 'cotacoes' se ela não existir, seguindo o modelo híbrido.
     * A estrutura contém apenas colunas essenciais para busca e uma coluna
     * NVARCHAR(MAX)
     * para armazenar o JSON completo, garantindo resiliência.
     */
    @Override
    protected void criarTabelaSeNaoExistir(final Connection conexao) throws SQLException {
        if (!verificarTabelaExiste(conexao, NOME_TABELA)) {
            logger.info("Criando tabela {} com arquitetura híbrida...", NOME_TABELA);

            final String sql = """
                    CREATE TABLE cotacoes (
                        -- Coluna de Chave Primária (Chave de Negócio)
                        sequence_code BIGINT PRIMARY KEY,

                        -- Colunas Essenciais para Indexação e Relatórios conforme docs/descobertas-endpoints/cotacoes.md
                        requested_at DATETIMEOFFSET,
                        operation_type NVARCHAR(100),
                        customer_doc NVARCHAR(14),
                        customer_name NVARCHAR(255),
                        origin_city NVARCHAR(100),
                        origin_state NVARCHAR(2),
                        destination_city NVARCHAR(100),
                        destination_state NVARCHAR(2),
                        price_table NVARCHAR(255),
                        volumes INT,
                        taxed_weight DECIMAL(18, 3),
                        invoices_value DECIMAL(18, 2),
                        total_value DECIMAL(18, 2),
                        user_name NVARCHAR(255),
                        branch_nickname NVARCHAR(255),
                        company_name NVARCHAR(255),
                        requester_name NVARCHAR(255),
                        real_weight NVARCHAR(50),
                        origin_postal_code NVARCHAR(10),
                        destination_postal_code NVARCHAR(10),
                        customer_nickname NVARCHAR(255),
                        sender_document NVARCHAR(14),
                        sender_nickname NVARCHAR(255),
                        receiver_document NVARCHAR(14),
                        receiver_nickname NVARCHAR(255),
                        disapprove_comments NVARCHAR(MAX),
                        freight_comments NVARCHAR(MAX),
                        discount_subtotal DECIMAL(18, 6),
                        itr_subtotal DECIMAL(18, 6),
                        tde_subtotal DECIMAL(18, 6),
                        collect_subtotal DECIMAL(18, 6),
                        delivery_subtotal DECIMAL(18, 6),
                        other_fees DECIMAL(18, 6),
                        cte_issued_at DATETIMEOFFSET,
                        nfse_issued_at DATETIMEOFFSET,

                        -- Coluna de Metadados para Resiliência e Completude
                        metadata NVARCHAR(MAX),

                        -- Coluna de Auditoria
                        data_extracao DATETIME2 DEFAULT GETDATE()
                    )
                    """;

            executarDDL(conexao, sql);
            logger.info("Tabela {} criada com sucesso.", NOME_TABELA);
        } else {
            // Adiciona colunas novas se a tabela já existir
            adicionarColunaSeNaoExistir(conexao, NOME_TABELA, "customer_nickname", "NVARCHAR(255)");
            adicionarColunaSeNaoExistir(conexao, NOME_TABELA, "sender_document", "NVARCHAR(14)");
            adicionarColunaSeNaoExistir(conexao, NOME_TABELA, "sender_nickname", "NVARCHAR(255)");
            adicionarColunaSeNaoExistir(conexao, NOME_TABELA, "receiver_document", "NVARCHAR(14)");
            adicionarColunaSeNaoExistir(conexao, NOME_TABELA, "receiver_nickname", "NVARCHAR(255)");
            adicionarColunaSeNaoExistir(conexao, NOME_TABELA, "disapprove_comments", "NVARCHAR(MAX)");
            adicionarColunaSeNaoExistir(conexao, NOME_TABELA, "freight_comments", "NVARCHAR(MAX)");
            adicionarColunaSeNaoExistir(conexao, NOME_TABELA, "discount_subtotal", "DECIMAL(18, 6)");
            adicionarColunaSeNaoExistir(conexao, NOME_TABELA, "itr_subtotal", "DECIMAL(18, 6)");
            adicionarColunaSeNaoExistir(conexao, NOME_TABELA, "tde_subtotal", "DECIMAL(18, 6)");
            adicionarColunaSeNaoExistir(conexao, NOME_TABELA, "collect_subtotal", "DECIMAL(18, 6)");
            adicionarColunaSeNaoExistir(conexao, NOME_TABELA, "delivery_subtotal", "DECIMAL(18, 6)");
            adicionarColunaSeNaoExistir(conexao, NOME_TABELA, "other_fees", "DECIMAL(18, 6)");
            adicionarColunaSeNaoExistir(conexao, NOME_TABELA, "cte_issued_at", "DATETIMEOFFSET");
            adicionarColunaSeNaoExistir(conexao, NOME_TABELA, "nfse_issued_at", "DATETIMEOFFSET");
        }
        if (!viewVerificada) {
            criarViewPowerBISeNaoExistir(conexao);
            viewVerificada = true;
            logger.info("View do Power BI verificada/atualizada para {}.", NOME_TABELA);
        }
    }

    private boolean colunaExiste(final Connection conexao, final String tabela, final String coluna) throws SQLException {
        final java.sql.DatabaseMetaData md = conexao.getMetaData();
        try (java.sql.ResultSet rs = md.getColumns(null, null, tabela.toUpperCase(), coluna.toUpperCase())) {
            return rs.next();
        }
    }

    private void adicionarColunaSeNaoExistir(final Connection conexao, final String tabela, final String coluna, final String definicao) throws SQLException {
        if (!colunaExiste(conexao, tabela, coluna)) {
            executarDDL(conexao, "ALTER TABLE " + tabela + " ADD " + coluna + " " + definicao);
            logger.info("Coluna adicionada: {}.{}", tabela, coluna);
        }
    }

    private void criarViewPowerBISeNaoExistir(final Connection conexao) throws SQLException {
        final String sqlView = """
                    CREATE OR ALTER VIEW dbo.vw_cotacoes_powerbi AS
                    SELECT
                        -- ========================================================================
                        -- GRUPO 1: DADOS REAIS (Mapeados da tabela 'cotacoes')
                        -- ========================================================================
                        sequence_code                                   AS [N° Cotação],
                        requested_at                                    AS [Data Cotação],
                        branch_nickname                                 AS [Filial],
                        requester_name                                  AS [Solicitante],
                        customer_name                                   AS [Cliente Pagador],
                        customer_doc                                    AS [CNPJ/CPF Cliente],
                        origin_city                                     AS [Cidade Origem],
                        origin_state                                    AS [UF Origem],
                        destination_city                                AS [Cidade Destino],
                        destination_state                               AS [UF Destino],
                        volumes                                         AS [Volume],
                        real_weight                                     AS [Peso real],
                        taxed_weight                                    AS [Peso taxado],
                        invoices_value                                  AS [Valor NF],
                        total_value                                     AS [Valor frete],
                        price_table                                     AS [Tabela],
                        user_name                                       AS [Usuário],
                        company_name                                    AS [Empresa],
                        operation_type                                  AS [Tipo de operação],
                        origin_postal_code                              AS [CEP Origem],
                        destination_postal_code                         AS [CEP Destino],
                    metadata                                         AS [Metadata],
                    data_extracao                                   AS [Data de extracao],

                        -- ========================================================================
                        -- GRUPO 2: CAMPOS DO METADATA (Extraídos do JSON 'metadata')
                        -- ========================================================================
                        CASE
                            WHEN cte_issued_at IS NOT NULL
                                OR nfse_issued_at IS NOT NULL
                            THEN 'Convertida'
                            WHEN disapprove_comments IS NOT NULL
                                AND LEN(disapprove_comments) > 0
                            THEN 'Reprovada'
                            ELSE 'Pendente'
                        END                                             AS [Status Conversão],

                        disapprove_comments                             AS [Motivo Perda],
                        freight_comments                                AS [Observações para o frete],
                        cte_issued_at                                   AS [CT-e/Data de emissão],
                        nfse_issued_at                                  AS [Nfse/Data de emissão],

                        customer_nickname                               AS [Pagador/Nome fantasia],
                        sender_document                                  AS [Remetente/CNPJ],
                        sender_nickname                                  AS [Remetente/Nome fantasia],
                        receiver_document                                AS [Destinatário/CNPJ],
                        receiver_nickname                                AS [Destinatário/Nome fantasia],

                        discount_subtotal                               AS [Descontos/Subtotal parcelas],
                        itr_subtotal                                    AS [Trechos/ITR],
                        tde_subtotal                                    AS [Trechos/TDE],
                        collect_subtotal                                AS [Trechos/Coleta],
                        delivery_subtotal                               AS [Trechos/Entrega],
                        other_fees                                      AS [Trechos/Outros valores],

                        operation_type                                  AS [qoe_qes_fon_name],
                        customer_doc                                    AS [qoe_cor_document],
                        customer_name                                   AS [qoe_cor_name],
                        origin_city                                     AS [qoe_qes_ony_name],
                        origin_state                                    AS [qoe_qes_ony_sae_code],
                        destination_city                                AS [qoe_qes_diy_name],
                        destination_state                               AS [qoe_qes_diy_sae_code],
                        price_table                                     AS [qoe_qes_cre_name],
                        volumes                                         AS [qoe_qes_invoices_volumes],
                        taxed_weight                                    AS [qoe_qes_taxed_weight],
                        invoices_value                                  AS [qoe_qes_invoices_value],
                        total_value                                     AS [qoe_qes_total],
                        origin_postal_code                              AS [qoe_qes_origin_postal_code],
                        destination_postal_code                         AS [qoe_qes_destination_postal_code],
                        real_weight                                     AS [qoe_qes_real_weight],
                        disapprove_comments                             AS [qoe_qes_disapprove_comments],
                        freight_comments                                AS [qoe_qes_freight_comments],
                        discount_subtotal                               AS [qoe_qes_fit_fdt_subtotal],
                        cte_issued_at                                   AS [qoe_qes_fit_fhe_cte_issued_at],
                        nfse_issued_at                                  AS [qoe_qes_fit_nse_issued_at],
                        requester_name                                  AS [requester_name],
                        itr_subtotal                                    AS [qoe_qes_itr_subtotal],
                        tde_subtotal                                    AS [qoe_qes_tde_subtotal],
                        collect_subtotal                                AS [qoe_qes_collect_subtotal],
                        delivery_subtotal                               AS [qoe_qes_delivery_subtotal],
                        other_fees                                      AS [qoe_qes_other_fees],
                        company_name                                    AS [qoe_crn_psn_name],
                        customer_nickname                               AS [qoe_cor_nickname],
                        branch_nickname                                 AS [qoe_crn_psn_nickname],
                        sender_document                                  AS [qoe_qes_sdr_document],
                        sender_nickname                                  AS [qoe_qes_sdr_nickname],
                        receiver_document                                AS [qoe_qes_rpt_document],
                        receiver_nickname                                AS [qoe_qes_rpt_nickname],
                        user_name                                       AS [qoe_uer_name],
                        requested_at                                    AS [requested_at],
                        sequence_code                                   AS [sequence_code]

                    FROM dbo.cotacoes;
                """;
        executarDDL(conexao, sqlView);
        try (PreparedStatement ps = conexao.prepareStatement("SELECT TOP 0 * FROM dbo.vw_cotacoes_powerbi");
             java.sql.ResultSet rs = ps.executeQuery()) {
            final java.sql.ResultSetMetaData md = rs.getMetaData();
            final StringBuilder cols = new StringBuilder();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                if (i > 1) cols.append(", ");
                cols.append(md.getColumnLabel(i));
            }
            logger.info("vw_cotacoes_powerbi colunas: {}", cols.toString());
        }
    }

    //     private void criarViewPowerBISeNaoExistir(final Connection conexao) throws SQLException {
    //     final String sqlView = """
    //         CREATE OR ALTER VIEW dbo.vw_cotacoes_powerbi AS
    //         SELECT
    //             sequence_code AS [Cotação],
    //             requested_at AS [Data],
    //             branch_nickname AS [Filial],
    //             requester_name AS [Solicitante],
    //             customer_name AS [Cliente],
    //             customer_doc AS [CNPJ Cliente],
    //             origin_city AS [Origem],
    //             origin_state AS [UF (Origem)],
    //             destination_city AS [Destino],
    //             destination_state AS [UF (Destino)],
    //             volumes AS [Volumes],
    //             real_weight AS [Peso Real],
    //             taxed_weight AS [Peso Taxado],
    //             invoices_value AS [Valor NF],
    //             total_value AS [Valor Frete],
    //             price_table AS [Tabela],
    //             user_name AS [Usuário],
    //             -- Campos derivados do metadata para status e evidências de conversão
    //             CASE
    //               WHEN JSON_VALUE(metadata, '$.qoe_qes_fit_fhe_cte_issued_at') IS NOT NULL
    //                    OR JSON_VALUE(metadata, '$.qoe_qes_fit_nse_issued_at') IS NOT NULL
    //                 THEN 'Convertida'
    //               WHEN JSON_VALUE(metadata, '$.qoe_qes_disapprove_comments') IS NOT NULL
    //                    AND LEN(JSON_VALUE(metadata, '$.qoe_qes_disapprove_comments')) > 0
    //                 THEN 'Reprovada'
    //               ELSE 'Pendente'
    //             END AS [Status Conversão],
    //             JSON_VALUE(metadata, '$.qoe_qes_disapprove_comments') AS [Motivo Perda],
    //             JSON_VALUE(metadata, '$.qoe_qes_fit_fhe_cte_issued_at') AS [CT-e/Data de emissão],
    //             JSON_VALUE(metadata, '$.qoe_qes_fit_nse_issued_at') AS [Nfse/Data de emissão],
    //             data_extracao AS [Data de extracao]
    //         FROM dbo.cotacoes;
    //     """;
    //     executarDDL(conexao, sqlView);
    // }

    /**
     * Executa a operação MERGE (UPSERT) para inserir ou atualizar uma cotação no
     * banco.
     * A lógica é segura e baseada na nova arquitetura de Entidade.
     */
    @Override
    protected int executarMerge(final Connection conexao, final CotacaoEntity cotacao) throws SQLException {
        // Para Cotações, o 'sequence_code' é a chave de negócio primária.
        if (cotacao.getSequenceCode() == null) {
            throw new SQLException("Não é possível executar o MERGE para Cotação sem um 'sequence_code'.");
        }

        final String sql = String.format(
                """
                        MERGE %s AS target
                        USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?))
                            AS source (
                                sequence_code, requested_at, operation_type, customer_doc, customer_name,
                                origin_city, origin_state, destination_city, destination_state, price_table,
                                volumes, taxed_weight, invoices_value, total_value, user_name, branch_nickname,
                                company_name, requester_name, real_weight, origin_postal_code, destination_postal_code,
                                customer_nickname, sender_document, sender_nickname, receiver_document, receiver_nickname,
                                disapprove_comments, freight_comments, discount_subtotal, itr_subtotal, tde_subtotal,
                                collect_subtotal, delivery_subtotal, other_fees, cte_issued_at, nfse_issued_at,
                                metadata, data_extracao
                            )
                        ON target.sequence_code = source.sequence_code
                        WHEN MATCHED THEN
                            UPDATE SET
                                requested_at = source.requested_at,
                                operation_type = source.operation_type,
                                customer_doc = source.customer_doc,
                                customer_name = source.customer_name,
                                origin_city = source.origin_city,
                                origin_state = source.origin_state,
                                destination_city = source.destination_city,
                                destination_state = source.destination_state,
                                price_table = source.price_table,
                                volumes = source.volumes,
                                taxed_weight = source.taxed_weight,
                                invoices_value = source.invoices_value,
                                total_value = source.total_value,
                                user_name = source.user_name,
                                branch_nickname = source.branch_nickname,
                                company_name = source.company_name,
                                requester_name = source.requester_name,
                                real_weight = source.real_weight,
                                origin_postal_code = source.origin_postal_code,
                                destination_postal_code = source.destination_postal_code,
                                customer_nickname = source.customer_nickname,
                                sender_document = source.sender_document,
                                sender_nickname = source.sender_nickname,
                                receiver_document = source.receiver_document,
                                receiver_nickname = source.receiver_nickname,
                                disapprove_comments = source.disapprove_comments,
                                freight_comments = source.freight_comments,
                                discount_subtotal = source.discount_subtotal,
                                itr_subtotal = source.itr_subtotal,
                                tde_subtotal = source.tde_subtotal,
                                collect_subtotal = source.collect_subtotal,
                                delivery_subtotal = source.delivery_subtotal,
                                other_fees = source.other_fees,
                                cte_issued_at = source.cte_issued_at,
                                nfse_issued_at = source.nfse_issued_at,
                                metadata = source.metadata,
                                data_extracao = source.data_extracao
                        WHEN NOT MATCHED THEN
                            INSERT (
                                sequence_code, requested_at, operation_type, customer_doc, customer_name,
                                origin_city, origin_state, destination_city, destination_state, price_table,
                                volumes, taxed_weight, invoices_value, total_value, user_name, branch_nickname,
                                company_name, requester_name, real_weight, origin_postal_code, destination_postal_code,
                                customer_nickname, sender_document, sender_nickname, receiver_document, receiver_nickname,
                                disapprove_comments, freight_comments, discount_subtotal, itr_subtotal, tde_subtotal,
                                collect_subtotal, delivery_subtotal, other_fees, cte_issued_at, nfse_issued_at,
                                metadata, data_extracao
                            )
                            VALUES (
                                source.sequence_code, source.requested_at, source.operation_type, source.customer_doc, source.customer_name,
                                source.origin_city, source.origin_state, source.destination_city, source.destination_state, source.price_table,
                                source.volumes, source.taxed_weight, source.invoices_value, source.total_value, source.user_name, source.branch_nickname,
                                source.company_name, source.requester_name, source.real_weight, source.origin_postal_code, source.destination_postal_code,
                                source.customer_nickname, source.sender_document, source.sender_nickname, source.receiver_document, source.receiver_nickname,
                                source.disapprove_comments, source.freight_comments, source.discount_subtotal, source.itr_subtotal, source.tde_subtotal,
                                source.collect_subtotal, source.delivery_subtotal, source.other_fees, source.cte_issued_at, source.nfse_issued_at,
                                source.metadata, source.data_extracao
                            );
                        """,
                NOME_TABELA);

        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            // Define os parâmetros de forma segura e na ordem correta conforme MERGE SQL
            int paramIndex = 1;
            statement.setObject(paramIndex++, cotacao.getSequenceCode(), Types.BIGINT);
            // Usar helper methods para tipos especiais
            if (cotacao.getRequestedAt() != null) {
                statement.setObject(paramIndex++, cotacao.getRequestedAt(), Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                statement.setNull(paramIndex++, Types.TIMESTAMP_WITH_TIMEZONE);
            }
            statement.setString(paramIndex++, cotacao.getOperationType());
            statement.setString(paramIndex++, cotacao.getCustomerDoc());
            statement.setString(paramIndex++, cotacao.getCustomerName());
            statement.setString(paramIndex++, cotacao.getOriginCity());
            statement.setString(paramIndex++, cotacao.getOriginState());
            statement.setString(paramIndex++, cotacao.getDestinationCity());
            statement.setString(paramIndex++, cotacao.getDestinationState());
            statement.setString(paramIndex++, cotacao.getPriceTable());
            statement.setObject(paramIndex++, cotacao.getVolumes(), Types.INTEGER);
            setBigDecimalParameter(statement, paramIndex++, cotacao.getTaxedWeight());
            setBigDecimalParameter(statement, paramIndex++, cotacao.getInvoicesValue());
            setBigDecimalParameter(statement, paramIndex++, cotacao.getTotalValue());
            statement.setString(paramIndex++, cotacao.getUserName());
            statement.setString(paramIndex++, cotacao.getBranchNickname());
            statement.setString(paramIndex++, cotacao.getCompanyName());
            statement.setString(paramIndex++, cotacao.getRequesterName());
            statement.setString(paramIndex++, cotacao.getRealWeight());
            statement.setString(paramIndex++, cotacao.getOriginPostalCode());
            statement.setString(paramIndex++, cotacao.getDestinationPostalCode());
            statement.setString(paramIndex++, cotacao.getCustomerNickname());
            statement.setString(paramIndex++, cotacao.getSenderDocument());
            statement.setString(paramIndex++, cotacao.getSenderNickname());
            statement.setString(paramIndex++, cotacao.getReceiverDocument());
            statement.setString(paramIndex++, cotacao.getReceiverNickname());
            statement.setString(paramIndex++, cotacao.getDisapproveComments());
            statement.setString(paramIndex++, cotacao.getFreightComments());
            setBigDecimalParameter(statement, paramIndex++, cotacao.getDiscountSubtotal());
            setBigDecimalParameter(statement, paramIndex++, cotacao.getItrSubtotal());
            setBigDecimalParameter(statement, paramIndex++, cotacao.getTdeSubtotal());
            setBigDecimalParameter(statement, paramIndex++, cotacao.getCollectSubtotal());
            setBigDecimalParameter(statement, paramIndex++, cotacao.getDeliverySubtotal());
            setBigDecimalParameter(statement, paramIndex++, cotacao.getOtherFees());
            if (cotacao.getCteIssuedAt() != null) {
                statement.setObject(paramIndex++, cotacao.getCteIssuedAt(), Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                statement.setNull(paramIndex++, Types.TIMESTAMP_WITH_TIMEZONE);
            }
            if (cotacao.getNfseIssuedAt() != null) {
                statement.setObject(paramIndex++, cotacao.getNfseIssuedAt(), Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                statement.setNull(paramIndex++, Types.TIMESTAMP_WITH_TIMEZONE);
            }
            statement.setString(paramIndex++, cotacao.getMetadata());
            setInstantParameter(statement, paramIndex++, Instant.now());

            if (paramIndex != 39) {
                throw new SQLException(
                        String.format("Número incorreto de parâmetros: esperado 38, definido %d", paramIndex - 1));
            }

            final int rowsAffected = statement.executeUpdate();

            // ✅ VERIFICAR rows affected
            if (rowsAffected == 0) {
                logger.error("❌ MERGE retornou 0 linhas para cotação sequence_code={}. " +
                        "Possível violação de constraint ou dados inválidos.",
                        cotacao.getSequenceCode());
                // Não lançar exceção aqui - deixar o AbstractRepository tratar
                return 0;
            }

            if (rowsAffected > 0) {
                logger.debug("✅ Cotação sequence_code={} salva com sucesso: {} linha(s) afetada(s)",
                        cotacao.getSequenceCode(), rowsAffected);
            }

            return rowsAffected;
        } catch (final SQLException e) {
            logger.error("❌ SQLException ao salvar cotação sequence_code={}: {} - SQLState: {} - ErrorCode: {}",
                    cotacao.getSequenceCode(), e.getMessage(), e.getSQLState(), e.getErrorCode(), e);
            throw e;
        }
    }
}
