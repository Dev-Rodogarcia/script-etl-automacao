package br.com.extrator.db.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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

                    -- Campos Expandidos (22 campos do CSV)
                    pagador_id BIGINT,
                    pagador_nome NVARCHAR(255),
                    remetente_id BIGINT,
                    remetente_nome NVARCHAR(255),
                    origem_cidade NVARCHAR(255),
                    origem_uf NVARCHAR(10),
                    destinatario_id BIGINT,
                    destinatario_nome NVARCHAR(255),
                    destino_cidade NVARCHAR(255),
                    destino_uf NVARCHAR(10),
                    filial_nome NVARCHAR(255),
                    numero_nota_fiscal NVARCHAR(100),
                    tabela_preco_nome NVARCHAR(255),
                    classificacao_nome NVARCHAR(255),
                    centro_custo_nome NVARCHAR(255),
                    usuario_nome NVARCHAR(255),
                    reference_number NVARCHAR(100),
                    invoices_total_volumes INT,
                    taxed_weight DECIMAL(18, 3),
                    real_weight DECIMAL(18, 3),
                    total_cubic_volume DECIMAL(18, 3),
                    subtotal DECIMAL(18, 2),

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
            USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?))
                AS source (id, servico_em, criado_em, status, modal, tipo_frete, valor_total, valor_notas, peso_notas, id_corporacao, id_cidade_destino, data_previsao_entrega,
                           pagador_id, pagador_nome, remetente_id, remetente_nome, origem_cidade, origem_uf, destinatario_id, destinatario_nome, destino_cidade, destino_uf,
                           filial_nome, numero_nota_fiscal, tabela_preco_nome, classificacao_nome, centro_custo_nome, usuario_nome, reference_number, invoices_total_volumes,
                           taxed_weight, real_weight, total_cubic_volume, subtotal, metadata, data_extracao)
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
                    pagador_id = source.pagador_id,
                    pagador_nome = source.pagador_nome,
                    remetente_id = source.remetente_id,
                    remetente_nome = source.remetente_nome,
                    origem_cidade = source.origem_cidade,
                    origem_uf = source.origem_uf,
                    destinatario_id = source.destinatario_id,
                    destinatario_nome = source.destinatario_nome,
                    destino_cidade = source.destino_cidade,
                    destino_uf = source.destino_uf,
                    filial_nome = source.filial_nome,
                    numero_nota_fiscal = source.numero_nota_fiscal,
                    tabela_preco_nome = source.tabela_preco_nome,
                    classificacao_nome = source.classificacao_nome,
                    centro_custo_nome = source.centro_custo_nome,
                    usuario_nome = source.usuario_nome,
                    reference_number = source.reference_number,
                    invoices_total_volumes = source.invoices_total_volumes,
                    taxed_weight = source.taxed_weight,
                    real_weight = source.real_weight,
                    total_cubic_volume = source.total_cubic_volume,
                    subtotal = source.subtotal,
                    metadata = source.metadata,
                    data_extracao = source.data_extracao
            WHEN NOT MATCHED THEN
                INSERT (id, servico_em, criado_em, status, modal, tipo_frete, valor_total, valor_notas, peso_notas, id_corporacao, id_cidade_destino, data_previsao_entrega,
                        pagador_id, pagador_nome, remetente_id, remetente_nome, origem_cidade, origem_uf, destinatario_id, destinatario_nome, destino_cidade, destino_uf,
                        filial_nome, numero_nota_fiscal, tabela_preco_nome, classificacao_nome, centro_custo_nome, usuario_nome, reference_number, invoices_total_volumes,
                        taxed_weight, real_weight, total_cubic_volume, subtotal, metadata, data_extracao)
                VALUES (source.id, source.servico_em, source.criado_em, source.status, source.modal, source.tipo_frete, source.valor_total, source.valor_notas, source.peso_notas, source.id_corporacao, source.id_cidade_destino, source.data_previsao_entrega,
                        source.pagador_id, source.pagador_nome, source.remetente_id, source.remetente_nome, source.origem_cidade, source.origem_uf, source.destinatario_id, source.destinatario_nome, source.destino_cidade, source.destino_uf,
                        source.filial_nome, source.numero_nota_fiscal, source.tabela_preco_nome, source.classificacao_nome, source.centro_custo_nome, source.usuario_nome, source.reference_number, source.invoices_total_volumes,
                        source.taxed_weight, source.real_weight, source.total_cubic_volume, source.subtotal, source.metadata, source.data_extracao);
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
            // Campos expandidos (22 campos do CSV)
            statement.setObject(paramIndex++, frete.getPagadorId(), Types.BIGINT);
            statement.setString(paramIndex++, frete.getPagadorNome());
            statement.setObject(paramIndex++, frete.getRemetenteId(), Types.BIGINT);
            statement.setString(paramIndex++, frete.getRemetenteNome());
            statement.setString(paramIndex++, frete.getOrigemCidade());
            statement.setString(paramIndex++, frete.getOrigemUf());
            statement.setObject(paramIndex++, frete.getDestinatarioId(), Types.BIGINT);
            statement.setString(paramIndex++, frete.getDestinatarioNome());
            statement.setString(paramIndex++, frete.getDestinoCidade());
            statement.setString(paramIndex++, frete.getDestinoUf());
            statement.setString(paramIndex++, frete.getFilialNome());
            statement.setString(paramIndex++, frete.getNumeroNotaFiscal());
            statement.setString(paramIndex++, frete.getTabelaPrecoNome());
            statement.setString(paramIndex++, frete.getClassificacaoNome());
            statement.setString(paramIndex++, frete.getCentroCustoNome());
            statement.setString(paramIndex++, frete.getUsuarioNome());
            statement.setString(paramIndex++, frete.getReferenceNumber());
            statement.setObject(paramIndex++, frete.getInvoicesTotalVolumes(), Types.INTEGER);
            statement.setBigDecimal(paramIndex++, frete.getTaxedWeight());
            statement.setBigDecimal(paramIndex++, frete.getRealWeight());
            statement.setBigDecimal(paramIndex++, frete.getTotalCubicVolume());
            statement.setBigDecimal(paramIndex++, frete.getSubtotal());
            statement.setString(paramIndex++, frete.getMetadata());
            setInstantParameter(statement, paramIndex++, Instant.now()); // UTC timestamp
            
            // Verificar se todos os parâmetros foram definidos (36 parâmetros = paramIndex final = 37)
            if (paramIndex != 37) {
                throw new SQLException(String.format("Número incorreto de parâmetros: esperado 36, definido %d", paramIndex - 1));
            }

            int rowsAffected = statement.executeUpdate();
            logger.debug("MERGE executado para Frete ID {}: {} linha(s) afetada(s)", frete.getId(), rowsAffected);
            return rowsAffected;
        }
    }
}