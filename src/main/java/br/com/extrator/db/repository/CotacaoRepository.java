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
 * Implementa a arquitetura de persistência híbrida: colunas-chave para indexação
 * e uma coluna de metadados para resiliência e completude dos dados.
 * Utiliza operações MERGE (UPSERT) com a chave de negócio (sequenceCode).
 */
public class CotacaoRepository extends AbstractRepository<CotacaoEntity> {
    private static final Logger logger = LoggerFactory.getLogger(CotacaoRepository.class);
    private static final String NOME_TABELA = "cotacoes";

    @Override
    protected String getNomeTabela() {
        return NOME_TABELA;
    }

    /**
     * Cria a tabela 'cotacoes' se ela não existir, seguindo o modelo híbrido.
     * A estrutura contém apenas colunas essenciais para busca e uma coluna NVARCHAR(MAX)
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

                    -- Coluna de Metadados para Resiliência e Completude
                    metadata NVARCHAR(MAX),

                    -- Coluna de Auditoria
                    data_extracao DATETIME2 DEFAULT GETDATE()
                )
                """;

            executarDDL(conexao, sql);
            logger.info("Tabela {} criada com sucesso.", NOME_TABELA);
        }
        criarViewPowerBISeNaoExistir(conexao);
    }

    private void criarViewPowerBISeNaoExistir(final Connection conexao) throws SQLException {
        final String sqlView = """
            CREATE OR ALTER VIEW dbo.vw_cotacoes_powerbi AS
            SELECT
                sequence_code AS [Cotação],
                requested_at AS [Data],
                branch_nickname AS [Filial],
                requester_name AS [Solicitante],
                customer_name AS [Cliente],
                customer_doc AS [CNPJ Cliente],
                origin_city AS [Origem],
                origin_state AS [UF (Origem)],
                destination_city AS [Destino],
                destination_state AS [UF (Destino)],
                volumes AS [Volumes],
                real_weight AS [Peso Real],
                taxed_weight AS [Peso Taxado],
                invoices_value AS [Valor NF],
                total_value AS [Valor Frete],
                price_table AS [Tabela],
                user_name AS [Usuário],
                -- Campos derivados do metadata para status e evidências de conversão
                CASE
                  WHEN JSON_VALUE(metadata, '$.qoe_qes_fit_fhe_cte_issued_at') IS NOT NULL
                       OR JSON_VALUE(metadata, '$.qoe_qes_fit_nse_issued_at') IS NOT NULL
                    THEN 'Convertida'
                  WHEN JSON_VALUE(metadata, '$.qoe_qes_disapprove_comments') IS NOT NULL
                       AND LEN(JSON_VALUE(metadata, '$.qoe_qes_disapprove_comments')) > 0
                    THEN 'Reprovada'
                  ELSE 'Pendente'
                END AS [Status Conversão],
                JSON_VALUE(metadata, '$.qoe_qes_disapprove_comments') AS [Motivo Perda],
                JSON_VALUE(metadata, '$.qoe_qes_fit_fhe_cte_issued_at') AS [CT-e / Data emissão],
                JSON_VALUE(metadata, '$.qoe_qes_fit_nse_issued_at') AS [NFS-e / Data emissão],
                data_extracao AS [Data de extracao]
            FROM dbo.cotacoes;
        """;
        executarDDL(conexao, sqlView);
    }

    /**
     * Executa a operação MERGE (UPSERT) para inserir ou atualizar uma cotação no banco.
     * A lógica é segura e baseada na nova arquitetura de Entidade.
     */
    @Override
    protected int executarMerge(final Connection conexao, final CotacaoEntity cotacao) throws SQLException {
        // Para Cotações, o 'sequence_code' é a chave de negócio primária.
        if (cotacao.getSequenceCode() == null) {
            throw new SQLException("Não é possível executar o MERGE para Cotação sem um 'sequence_code'.");
        }

        final String sql = String.format("""
            MERGE %s AS target
            USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?))
                AS source (sequence_code, requested_at, operation_type, customer_doc, customer_name, origin_city, origin_state, destination_city, destination_state, price_table, volumes, taxed_weight, invoices_value, total_value, user_name, branch_nickname, company_name, requester_name, real_weight, origin_postal_code, destination_postal_code, metadata, data_extracao)
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
                    metadata = source.metadata,
                    data_extracao = source.data_extracao
            WHEN NOT MATCHED THEN
                INSERT (sequence_code, requested_at, operation_type, customer_doc, customer_name, origin_city, origin_state, destination_city, destination_state, price_table, volumes, taxed_weight, invoices_value, total_value, user_name, branch_nickname, company_name, requester_name, real_weight, origin_postal_code, destination_postal_code, metadata, data_extracao)
                VALUES (source.sequence_code, source.requested_at, source.operation_type, source.customer_doc, source.customer_name, source.origin_city, source.origin_state, source.destination_city, source.destination_state, source.price_table, source.volumes, source.taxed_weight, source.invoices_value, source.total_value, source.user_name, source.branch_nickname, source.company_name, source.requester_name, source.real_weight, source.origin_postal_code, source.destination_postal_code, source.metadata, source.data_extracao);
            """, NOME_TABELA);

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
            statement.setString(paramIndex++, cotacao.getMetadata());
            setInstantParameter(statement, paramIndex++, Instant.now()); // UTC timestamp
            
            // Verificar se todos os parâmetros foram definidos (23 parâmetros = paramIndex final = 24)
            if (paramIndex != 24) {
                throw new SQLException(String.format("Número incorreto de parâmetros: esperado 23, definido %d", paramIndex - 1));
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
