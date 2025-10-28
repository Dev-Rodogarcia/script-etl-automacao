package br.com.extrator.db.repository;

import br.com.extrator.db.entity.FaturaAPagarEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Repositório para operações de persistência da entidade FaturaAPagarEntity.
 * Implementa a arquitetura de persistência híbrida: colunas-chave para indexação
 * e colunas de metadados para resiliência e completude dos dados.
 * Utiliza operações MERGE (UPSERT) com chave primária (id) ou chave de negócio (documentNumber).
 */
public class FaturaAPagarRepository extends AbstractRepository<FaturaAPagarEntity> {
    private static final Logger logger = LoggerFactory.getLogger(FaturaAPagarRepository.class);
    private static final String NOME_TABELA = "faturas_a_pagar";

    @Override
    protected String getNomeTabela() {
        return NOME_TABELA;
    }

    /**
     * Cria a tabela 'faturas_a_pagar' se ela não existir, seguindo o modelo híbrido.
     * A estrutura contém apenas colunas essenciais para busca e duas colunas NVARCHAR(MAX)
     * para armazenar os dados JSON completos, garantindo resiliência.
     */
    @Override
    protected void criarTabelaSeNaoExistir(Connection conexao) throws SQLException {
        if (!verificarTabelaExiste(conexao, NOME_TABELA)) {
            logger.info("Criando tabela {} com arquitetura híbrida...", NOME_TABELA);

            String sql = """
                CREATE TABLE faturas_a_pagar (
                    -- Colunas de Chave
                    id BIGINT PRIMARY KEY,
                    document_number NVARCHAR(100),

                    -- Colunas Essenciais para Indexação e Relatórios
                    issue_date DATE,
                    due_date DATE,
                    total_value DECIMAL(18, 2),
                    receiver_cnpj NVARCHAR(14),
                    receiver_name NVARCHAR(255),
                    invoice_type NVARCHAR(100),

                    -- Colunas de Metadados para Resiliência e Completude
                    header_metadata NVARCHAR(MAX),
                    installments_metadata NVARCHAR(MAX),

                    -- Coluna de Auditoria
                    data_extracao DATETIME2 DEFAULT GETDATE(),

                    -- Constraint para chave de negócio
                    UNIQUE (document_number)
                )
                """;

            executarDDL(conexao, sql);
            logger.info("Tabela {} criada com sucesso.", NOME_TABELA);
        }
    }

    /**
     * Executa a operação MERGE (UPSERT) para inserir ou atualizar uma fatura no banco.
     * A lógica é segura e baseada na nova arquitetura de Entidade.
     */
    @Override
    protected int executarMerge(Connection conexao, FaturaAPagarEntity fatura) throws SQLException {
        // Define a cláusula de junção (ON) do MERGE, priorizando o ID primário
        // e usando a chave de negócio (document_number) como alternativa.
        String onClause;
        if (fatura.getId() != null) {
            onClause = "ON target.id = source.id";
        } else if (fatura.getDocumentNumber() != null && !fatura.getDocumentNumber().trim().isEmpty()) {
            onClause = "ON target.document_number = source.document_number";
        } else {
            // Se não houver chave, não é possível fazer o MERGE. Lança uma exceção.
            throw new SQLException("Não é possível executar o MERGE para Fatura a Pagar sem um ID ou Número de Documento.");
        }

        String sql = String.format("""
            MERGE %s AS target
            USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?))
                AS source (id, document_number, issue_date, due_date, total_value, receiver_cnpj, receiver_name, invoice_type, header_metadata, installments_metadata)
            %s
            WHEN MATCHED THEN
                UPDATE SET
                    issue_date = source.issue_date,
                    due_date = source.due_date,
                    total_value = source.total_value,
                    receiver_cnpj = source.receiver_cnpj,
                    receiver_name = source.receiver_name,
                    invoice_type = source.invoice_type,
                    header_metadata = source.header_metadata,
                    installments_metadata = source.installments_metadata,
                    data_extracao = GETDATE()
            WHEN NOT MATCHED THEN
                INSERT (id, document_number, issue_date, due_date, total_value, receiver_cnpj, receiver_name, invoice_type, header_metadata, installments_metadata)
                VALUES (source.id, source.document_number, source.issue_date, source.due_date, source.total_value, source.receiver_cnpj, source.receiver_name, source.invoice_type, source.header_metadata, source.installments_metadata);
            """, NOME_TABELA, onClause);

        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            // Define os parâmetros de forma segura e na ordem correta.
            int paramIndex = 1;
            statement.setObject(paramIndex++, fatura.getId(), Types.BIGINT);
            statement.setString(paramIndex++, fatura.getDocumentNumber());
            statement.setObject(paramIndex++, fatura.getIssueDate(), Types.DATE);
            statement.setObject(paramIndex++, fatura.getDueDate(), Types.DATE);
            statement.setBigDecimal(paramIndex++, fatura.getTotalValue());
            statement.setString(paramIndex++, fatura.getReceiverCnpj());
            statement.setString(paramIndex++, fatura.getReceiverName());
            statement.setString(paramIndex++, fatura.getInvoiceType());
            statement.setString(paramIndex++, fatura.getHeaderMetadata());
            statement.setString(paramIndex++, fatura.getInstallmentsMetadata());

            int rowsAffected = statement.executeUpdate();
            logger.debug("MERGE executado para Fatura a Pagar ID {}: {} linha(s) afetada(s)", fatura.getId(), rowsAffected);
            return rowsAffected;
        }
    }
}