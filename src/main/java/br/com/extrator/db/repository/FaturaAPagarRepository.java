package br.com.extrator.db.repository;

import br.com.extrator.db.entity.FaturaAPagarEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;

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
    protected void criarTabelaSeNaoExistir(final Connection conexao) throws SQLException {
        if (!verificarTabelaExiste(conexao, NOME_TABELA)) {
            logger.info("Criando tabela {} com arquitetura híbrida...", NOME_TABELA);

            final String sql = """
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

                    -- NOVOS CAMPOS DISPONÍVEIS (14/24)
                    cnpj_filial NVARCHAR(14),
                    filial NVARCHAR(255),
                    observacoes NVARCHAR(MAX),
                    conta_contabil NVARCHAR(255),
                    centro_custo NVARCHAR(500),
                    status NVARCHAR(50),
                    forma_pagamento NVARCHAR(100),

                    -- CAMPOS FUTUROS (10/24) - Preparados para integração futura
                    sequencia NVARCHAR(50),
                    cheque NVARCHAR(50),
                    vencimento_original DATE,
                    competencia NVARCHAR(7),
                    data_baixa DATE,
                    data_liquidacao DATE,
                    banco_pagamento NVARCHAR(255),
                    conta_pagamento NVARCHAR(100),
                    descricao_despesa NVARCHAR(MAX),

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
    protected int executarMerge(final Connection conexao, final FaturaAPagarEntity fatura) throws SQLException {
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

        final String sql = String.format("""
            MERGE %s AS target
            USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?))
                AS source (id, document_number, issue_date, due_date, total_value, receiver_cnpj, receiver_name, invoice_type,
                          cnpj_filial, filial, observacoes, conta_contabil, centro_custo, status, forma_pagamento,
                          header_metadata, installments_metadata, data_extracao,
                          sequencia, cheque, vencimento_original, competencia, data_baixa)
            %s
            WHEN MATCHED THEN
                UPDATE SET
                    issue_date = source.issue_date,
                    due_date = source.due_date,
                    total_value = source.total_value,
                    receiver_cnpj = source.receiver_cnpj,
                    receiver_name = source.receiver_name,
                    invoice_type = source.invoice_type,
                    cnpj_filial = source.cnpj_filial,
                    filial = source.filial,
                    observacoes = source.observacoes,
                    conta_contabil = source.conta_contabil,
                    centro_custo = source.centro_custo,
                    status = source.status,
                    forma_pagamento = source.forma_pagamento,
                    header_metadata = source.header_metadata,
                    installments_metadata = source.installments_metadata,
                    data_extracao = source.data_extracao,
                    sequencia = source.sequencia,
                    cheque = source.cheque,
                    vencimento_original = source.vencimento_original,
                    competencia = source.competencia,
                    data_baixa = source.data_baixa
            WHEN NOT MATCHED THEN
                INSERT (id, document_number, issue_date, due_date, total_value, receiver_cnpj, receiver_name, invoice_type,
                       cnpj_filial, filial, observacoes, conta_contabil, centro_custo, status, forma_pagamento,
                       header_metadata, installments_metadata, data_extracao,
                       sequencia, cheque, vencimento_original, competencia, data_baixa)
                VALUES (source.id, source.document_number, source.issue_date, source.due_date, source.total_value,
                       source.receiver_cnpj, source.receiver_name, source.invoice_type,
                       source.cnpj_filial, source.filial, source.observacoes, source.conta_contabil, source.centro_custo, source.status, source.forma_pagamento,
                       source.header_metadata, source.installments_metadata, source.data_extracao,
                       source.sequencia, source.cheque, source.vencimento_original, source.competencia, source.data_baixa);
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
            
            // Novos campos disponíveis
            statement.setString(paramIndex++, fatura.getCnpjFilial());
            statement.setString(paramIndex++, fatura.getFilial());
            statement.setString(paramIndex++, fatura.getObservacoes());
            statement.setString(paramIndex++, fatura.getContaContabil());
            statement.setString(paramIndex++, fatura.getCentroCusto());
            statement.setString(paramIndex++, fatura.getStatus());
            statement.setString(paramIndex++, fatura.getFormaPagamento());
            
            // Metadados
            statement.setString(paramIndex++, fatura.getHeaderMetadata());
            statement.setString(paramIndex++, fatura.getInstallmentsMetadata());
            statement.setTimestamp(paramIndex++, Timestamp.from(Instant.now())); // UTC timestamp
            
            // Campos futuros (apenas alguns para o MERGE)
            statement.setString(paramIndex++, fatura.getSequencia());
            statement.setString(paramIndex++, fatura.getCheque());
            statement.setObject(paramIndex++, fatura.getVencimentoOriginal(), Types.DATE);
            statement.setString(paramIndex++, fatura.getCompetencia());
            statement.setObject(paramIndex++, fatura.getDataBaixa(), Types.DATE);

            final int rowsAffected = statement.executeUpdate();
            logger.debug("MERGE executado para Fatura a Pagar ID {}: {} linha(s) afetada(s)", fatura.getId(), rowsAffected);
            return rowsAffected;
        }
    }
}