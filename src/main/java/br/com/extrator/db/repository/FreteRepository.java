package br.com.extrator.db.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.db.entity.FreteEntity;

/**
 * Repositório para operações de persistência da entidade FreteEntity.
 * Implementa a arquitetura de persistência híbrida: colunas-chave para indexação
 * e uma coluna de metadados para resiliência e completude dos dados.
 * Utiliza operações MERGE (UPSERT) com a chave primária (id) do frete.
 */
public class FreteRepository extends AbstractRepository<FreteEntity> {
    private static final Logger logger = LoggerFactory.getLogger(FreteRepository.class);
    private static final String NOME_TABELA = "fretes";

    @Override
    protected String getNomeTabela() {
        return NOME_TABELA;
    }

    /**
     * Cria a tabela 'fretes' se ela não existir, seguindo o modelo híbrido.
     * A estrutura contém apenas colunas essenciais para busca e uma coluna NVARCHAR(MAX)
     * para armazenar o JSON completo, garantindo resiliência.
     */
    @Override
    protected void criarTabelaSeNaoExistir(Connection conexao) throws SQLException {
        if (!verificarTabelaExiste(conexao, NOME_TABELA)) {
            logger.info("Criando tabela {} com arquitetura híbrida...", NOME_TABELA);

            String sql = """
                CREATE TABLE fretes (
                    -- Coluna de Chave Primária
                    id BIGINT PRIMARY KEY,

                    -- Colunas Essenciais "Promovidas" para Indexação e Relatórios
                    servico_em DATETIMEOFFSET,
                    criado_em DATETIMEOFFSET,
                    status NVARCHAR(50),
                    modal NVARCHAR(50),
                    tipo_frete NVARCHAR(100),
                    valor_total DECIMAL(18, 2),
                    valor_notas DECIMAL(18, 2),
                    peso_notas DECIMAL(18, 3),
                    id_corporacao BIGINT,
                    id_cidade_destino BIGINT,
                    data_previsao_entrega DATE,

                    -- Coluna de Metadados para Resiliência e Completude
                    metadata NVARCHAR(MAX),

                    -- Coluna de Auditoria
                    data_extracao DATETIME2 DEFAULT GETDATE()
                )
                """;

            executarDDL(conexao, sql);
            logger.info("Tabela {} criada com sucesso.", NOME_TABELA);
        }
    }

    /**
     * Executa a operação MERGE (UPSERT) para inserir ou atualizar um frete no banco.
     * A lógica é segura e baseada na nova arquitetura de Entidade.
     */
    @Override
    protected int executarMerge(Connection conexao, FreteEntity frete) throws SQLException {
        // Para Fretes, o 'id' é a única chave confiável para o MERGE.
        if (frete.getId() == null) {
            throw new SQLException("Não é possível executar o MERGE para Frete sem um ID.");
        }

        String sql = String.format("""
            MERGE %s AS target
            USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?))
                AS source (id, servico_em, criado_em, status, modal, tipo_frete, valor_total, valor_notas, peso_notas, id_corporacao, id_cidade_destino, data_previsao_entrega, metadata, data_extracao)
            ON target.id = source.id
            WHEN MATCHED THEN
                UPDATE SET
                    servico_em = source.servico_em,
                    criado_em = source.criado_em,
                    status = source.status,
                    modal = source.modal,
                    tipo_frete = source.tipo_frete,
                    valor_total = source.valor_total,
                    valor_notas = source.valor_notas,
                    peso_notas = source.peso_notas,
                    id_corporacao = source.id_corporacao,
                    id_cidade_destino = source.id_cidade_destino,
                    data_previsao_entrega = source.data_previsao_entrega,
                    metadata = source.metadata,
                    data_extracao = source.data_extracao
            WHEN NOT MATCHED THEN
                INSERT (id, servico_em, criado_em, status, modal, tipo_frete, valor_total, valor_notas, peso_notas, id_corporacao, id_cidade_destino, data_previsao_entrega, metadata, data_extracao)
                VALUES (source.id, source.servico_em, source.criado_em, source.status, source.modal, source.tipo_frete, source.valor_total, source.valor_notas, source.peso_notas, source.id_corporacao, source.id_cidade_destino, source.data_previsao_entrega, source.metadata, source.data_extracao);
            """, NOME_TABELA);

        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            // Define os parâmetros de forma segura e na ordem correta.
            int paramIndex = 1;
            statement.setObject(paramIndex++, frete.getId(), Types.BIGINT);
            statement.setObject(paramIndex++, frete.getServicoEm(), Types.TIMESTAMP_WITH_TIMEZONE);
            statement.setObject(paramIndex++, frete.getCriadoEm(), Types.TIMESTAMP_WITH_TIMEZONE);
            statement.setString(paramIndex++, frete.getStatus());
            statement.setString(paramIndex++, frete.getModal());
            statement.setString(paramIndex++, frete.getTipoFrete());
            statement.setBigDecimal(paramIndex++, frete.getValorTotal());
            statement.setBigDecimal(paramIndex++, frete.getValorNotas());
            statement.setBigDecimal(paramIndex++, frete.getPesoNotas());
            statement.setObject(paramIndex++, frete.getIdCorporacao(), Types.BIGINT);
            statement.setObject(paramIndex++, frete.getIdCidadeDestino(), Types.BIGINT);
            statement.setObject(paramIndex++, frete.getDataPrevisaoEntrega(), Types.DATE);
            statement.setString(paramIndex++, frete.getMetadata());
            statement.setTimestamp(paramIndex++, Timestamp.from(Instant.now())); // UTC timestamp

            int rowsAffected = statement.executeUpdate();
            logger.debug("MERGE executado para Frete ID {}: {} linha(s) afetada(s)", frete.getId(), rowsAffected);
            return rowsAffected;
        }
    }
}