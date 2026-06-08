package br.com.extrator.aplicacao.materializacao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.extrator.suporte.banco.GerenciadorConexao;

public class FatoMaterializacaoRepository {
    public List<FatoMaterializacaoProcedureResultado> executarProcedures(final List<String> procedures,
                                                                        final int timeoutSegundos) throws SQLException {
        final List<FatoMaterializacaoProcedureResultado> resultados = new ArrayList<>();
        for (final String procedure : procedures) {
            resultados.add(executarProcedure(procedure, timeoutSegundos));
        }
        return resultados;
    }

    private FatoMaterializacaoProcedureResultado executarProcedure(final String procedure,
                                                                   final int timeoutSegundos) throws SQLException {
        final Instant inicio = Instant.now();
        try (Connection conexao = GerenciadorConexao.obterConexao();
             CallableStatement statement = conexao.prepareCall("{call " + procedure + "}")) {
            statement.setQueryTimeout(timeoutSegundos);
            final boolean possuiResultado = statement.execute();
            if (possuiResultado) {
                try (ResultSet rs = statement.getResultSet()) {
                    if (rs != null && rs.next()) {
                        return new FatoMaterializacaoProcedureResultado(
                            procedure,
                            rs.getLong("linhas_inseridas"),
                            rs.getLong("linhas_atualizadas"),
                            obterSnapshot(rs),
                            Duration.between(inicio, Instant.now())
                        );
                    }
                }
            }
            return new FatoMaterializacaoProcedureResultado(
                procedure,
                0L,
                0L,
                null,
                Duration.between(inicio, Instant.now())
            );
        }
    }

    private LocalDateTime obterSnapshot(final ResultSet rs) throws SQLException {
        final Timestamp snapshot = rs.getTimestamp("snapshot_em");
        return snapshot == null ? null : snapshot.toLocalDateTime();
    }
}
