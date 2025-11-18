package br.com.extrator.db.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.db.entity.ContasAPagarDataExportEntity;
import br.com.extrator.util.GerenciadorConexao;

/**
 * Repository para persistência de dados de Contas a Pagar (Data Export) no SQL Server.
 * Tabela: contas_a_pagar
 * Template ID: 8636
 */
public class ContasAPagarRepository {
    private static final Logger logger = LoggerFactory.getLogger(ContasAPagarRepository.class);
    private static final String NOME_TABELA = "contas_a_pagar";
    
    /**
     * Cria a tabela se não existir.
     */
    public void criarTabelaSeNaoExistir() throws SQLException {
        final String sql = """
            IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'contas_a_pagar')
            BEGIN
                CREATE TABLE contas_a_pagar (
                    sequence_code BIGINT PRIMARY KEY,
                    document_number VARCHAR(100),
                    issue_date DATE,
                    tipo_lancamento NVARCHAR(100),
                    valor_original DECIMAL(18,2),
                    valor_juros DECIMAL(18,2),
                    valor_desconto DECIMAL(18,2),
                    valor_a_pagar DECIMAL(18,2),
                    valor_pago DECIMAL(18,2),
                    status_pagamento NVARCHAR(50),
                    mes_competencia INT,
                    ano_competencia INT,
                    data_criacao DATETIMEOFFSET,
                    data_liquidacao DATE,
                    data_transacao DATE,
                    nome_fornecedor NVARCHAR(255),
                    nome_filial NVARCHAR(255),
                    nome_centro_custo NVARCHAR(255),
                    valor_centro_custo DECIMAL(18,2),
                    classificacao_contabil NVARCHAR(100),
                    descricao_contabil NVARCHAR(255),
                    valor_contabil DECIMAL(18,2),
                    area_lancamento NVARCHAR(255),
                    observacoes NVARCHAR(MAX),
                    descricao_despesa NVARCHAR(MAX),
                    nome_usuario NVARCHAR(255),
                    reconciliado BIT,
                    metadata NVARCHAR(MAX),
                    data_extracao DATETIME2 DEFAULT GETDATE()
                );
                CREATE INDEX IX_fp_data_export_issue_date ON contas_a_pagar(issue_date);
                CREATE INDEX IX_fp_data_export_status ON contas_a_pagar(status_pagamento);
                CREATE INDEX IX_fp_data_export_fornecedor ON contas_a_pagar(nome_fornecedor);
                CREATE INDEX IX_fp_data_export_filial ON contas_a_pagar(nome_filial);
                CREATE INDEX IX_fp_data_export_competencia ON contas_a_pagar(ano_competencia, mes_competencia);
            END
            """;
        
        try (Connection conn = GerenciadorConexao.obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
            logger.info("✓ Tabela {} verificada/criada com sucesso", NOME_TABELA);
        }
    }
    
    /**
     * Salva lista de entidades usando MERGE (INSERT ou UPDATE).
     */
    public int salvar(final List<ContasAPagarDataExportEntity> entidades) throws SQLException {
        if (entidades == null || entidades.isEmpty()) {
            logger.warn("Lista de {} vazia, nada a salvar", NOME_TABELA);
            return 0;
        }
        
        criarTabelaSeNaoExistir();
        
        final String sqlMerge = """
            MERGE INTO contas_a_pagar AS target
            USING (SELECT ? AS sequence_code) AS source
            ON target.sequence_code = source.sequence_code
            WHEN MATCHED THEN
                UPDATE SET
                    document_number = ?,
                    issue_date = ?,
                    tipo_lancamento = ?,
                    valor_original = ?,
                    valor_juros = ?,
                    valor_desconto = ?,
                    valor_a_pagar = ?,
                    valor_pago = ?,
                    status_pagamento = ?,
                    mes_competencia = ?,
                    ano_competencia = ?,
                    data_criacao = ?,
                    data_liquidacao = ?,
                    data_transacao = ?,
                    nome_fornecedor = ?,
                    nome_filial = ?,
                    nome_centro_custo = ?,
                    valor_centro_custo = ?,
                    classificacao_contabil = ?,
                    descricao_contabil = ?,
                    valor_contabil = ?,
                    area_lancamento = ?,
                    observacoes = ?,
                    descricao_despesa = ?,
                    nome_usuario = ?,
                    reconciliado = ?,
                    metadata = ?,
                    data_extracao = GETDATE()
            WHEN NOT MATCHED THEN
                INSERT (sequence_code, document_number, issue_date, tipo_lancamento,
                        valor_original, valor_juros, valor_desconto, valor_a_pagar, valor_pago,
                        status_pagamento, mes_competencia, ano_competencia,
                        data_criacao, data_liquidacao, data_transacao,
                        nome_fornecedor, nome_filial, nome_centro_custo, valor_centro_custo,
                        classificacao_contabil, descricao_contabil, valor_contabil, area_lancamento,
                        observacoes, descricao_despesa, nome_usuario, reconciliado,
                        metadata, data_extracao)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE());
            """;
        
        int totalProcessados = 0;
        
        try (Connection conn = GerenciadorConexao.obterConexao()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(sqlMerge)) {
                
                for (final ContasAPagarDataExportEntity entity : entidades) {
                    // Validação
                    if (entity.getSequenceCode() == null) {
                        logger.warn("Entidade com sequence_code null ignorada");
                        continue;
                    }
                    
                    // Parâmetros do MERGE (ON)
                    ps.setLong(1, entity.getSequenceCode());
                    
                    // Parâmetros do UPDATE
                    ps.setString(2, entity.getDocumentNumber());
                    ps.setObject(3, entity.getIssueDate());
                    ps.setString(4, entity.getTipoLancamento());
                    ps.setBigDecimal(5, entity.getValorOriginal());
                    ps.setBigDecimal(6, entity.getValorJuros());
                    ps.setBigDecimal(7, entity.getValorDesconto());
                    ps.setBigDecimal(8, entity.getValorAPagar());
                    ps.setBigDecimal(9, entity.getValorPago());
                    ps.setString(10, entity.getStatusPagamento());
                    ps.setObject(11, entity.getMesCompetencia());
                    ps.setObject(12, entity.getAnoCompetencia());
                    ps.setObject(13, entity.getDataCriacao());
                    ps.setObject(14, entity.getDataLiquidacao());
                    ps.setObject(15, entity.getDataTransacao());
                    ps.setString(16, entity.getNomeFornecedor());
                    ps.setString(17, entity.getNomeFilial());
                    ps.setString(18, entity.getNomeCentroCusto());
                    ps.setBigDecimal(19, entity.getValorCentroCusto());
                    ps.setString(20, entity.getClassificacaoContabil());
                    ps.setString(21, entity.getDescricaoContabil());
                    ps.setBigDecimal(22, entity.getValorContabil());
                    ps.setString(23, entity.getAreaLancamento());
                    ps.setString(24, entity.getObservacoes());
                    ps.setString(25, entity.getDescricaoDespesa());
                    ps.setString(26, entity.getNomeUsuario());
                    ps.setObject(27, entity.getReconciliado());
                    ps.setString(28, entity.getMetadata());
                    
                    // Parâmetros do INSERT (mesmos valores)
                    ps.setLong(29, entity.getSequenceCode());
                    ps.setString(30, entity.getDocumentNumber());
                    ps.setObject(31, entity.getIssueDate());
                    ps.setString(32, entity.getTipoLancamento());
                    ps.setBigDecimal(33, entity.getValorOriginal());
                    ps.setBigDecimal(34, entity.getValorJuros());
                    ps.setBigDecimal(35, entity.getValorDesconto());
                    ps.setBigDecimal(36, entity.getValorAPagar());
                    ps.setBigDecimal(37, entity.getValorPago());
                    ps.setString(38, entity.getStatusPagamento());
                    ps.setObject(39, entity.getMesCompetencia());
                    ps.setObject(40, entity.getAnoCompetencia());
                    ps.setObject(41, entity.getDataCriacao());
                    ps.setObject(42, entity.getDataLiquidacao());
                    ps.setObject(43, entity.getDataTransacao());
                    ps.setString(44, entity.getNomeFornecedor());
                    ps.setString(45, entity.getNomeFilial());
                    ps.setString(46, entity.getNomeCentroCusto());
                    ps.setBigDecimal(47, entity.getValorCentroCusto());
                    ps.setString(48, entity.getClassificacaoContabil());
                    ps.setString(49, entity.getDescricaoContabil());
                    ps.setBigDecimal(50, entity.getValorContabil());
                    ps.setString(51, entity.getAreaLancamento());
                    ps.setString(52, entity.getObservacoes());
                    ps.setString(53, entity.getDescricaoDespesa());
                    ps.setString(54, entity.getNomeUsuario());
                    ps.setObject(55, entity.getReconciliado());
                    ps.setString(56, entity.getMetadata());
                    
                    final int rowsAffected = ps.executeUpdate();
                    if (rowsAffected > 0) {
                        totalProcessados++;
                    }
                }
                
                conn.commit();
                logger.info("✓ Processados {}/{} registros em {}", 
                    totalProcessados, entidades.size(), NOME_TABELA);
                
            } catch (final SQLException e) {
                conn.rollback();
                logger.error("Erro ao salvar em {}: {}", NOME_TABELA, e.getMessage());
                throw e;
            }
        }
        
        return totalProcessados;
    }
}
