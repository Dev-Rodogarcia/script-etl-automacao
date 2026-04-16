package br.com.extrator.aplicacao.validacao;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import br.com.extrator.dominio.dataexport.faturaporcliente.FaturaPorClienteDTO;
import br.com.extrator.dominio.dataexport.inventario.InventarioDTO;
import br.com.extrator.integracao.dataexport.support.Deduplicator;
import br.com.extrator.integracao.mapeamento.dataexport.faturaporcliente.FaturaPorClienteMapper;
import br.com.extrator.integracao.mapeamento.dataexport.inventario.InventarioMapper;
import br.com.extrator.persistencia.entidade.FaturaPorClienteEntity;
import br.com.extrator.persistencia.entidade.InventarioEntity;
import br.com.extrator.suporte.banco.GerenciadorConexao;
import br.com.extrator.suporte.mapeamento.MapperUtil;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

class CurlBancoPeriodoFechadoOperationalProofTest {

    private static final DateTimeFormatter REPORT_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final Pattern PAGE_FILE_PATTERN = Pattern.compile("-(?:page)(\\d+)\\.json$");

    @Test
    void deveConfirmarParidadeOperacionalViaPayloadsCurl() throws Exception {
        final String rawDirProperty = System.getProperty("operationalProof.rawDir");
        final String executionUuid = System.getProperty("operationalProof.executionUuid");

        Assumptions.assumeTrue(rawDirProperty != null && !rawDirProperty.isBlank(),
            "Defina -DoperationalProof.rawDir para executar a prova operacional.");
        Assumptions.assumeTrue(executionUuid != null && !executionUuid.isBlank(),
            "Defina -DoperationalProof.executionUuid para executar a prova operacional.");

        final Path rawDir = Paths.get(rawDirProperty);
        Assumptions.assumeTrue(Files.isDirectory(rawDir),
            "Diretorio de payloads curl nao encontrado: " + rawDir.toAbsolutePath());

        final ValidacaoApiBanco24hDetalhadaMetadataHasher metadataHasher =
            new ValidacaoApiBanco24hDetalhadaMetadataHasher();

        final ApiState faturasApi = carregarFaturasPorCliente(rawDir, metadataHasher);
        final ApiState inventarioApi = carregarInventario(rawDir, metadataHasher);

        final ComparisonResult faturasResultado;
        final ComparisonResult inventarioResultado;
        try (Connection conexao = GerenciadorConexao.obterConexao()) {
            final DbState faturasDb = carregarEstadoBanco(
                conexao,
                executionUuid,
                ConstantesEntidades.FATURAS_POR_CLIENTE,
                "SELECT unique_id AS chave, metadata FROM dbo.faturas_por_cliente WHERE data_extracao >= ? AND data_extracao <= ? AND unique_id IS NOT NULL ORDER BY unique_id",
                metadataHasher
            );
            final DbState inventarioDb = carregarEstadoBanco(
                conexao,
                executionUuid,
                ConstantesEntidades.INVENTARIO,
                "SELECT identificador_unico AS chave, metadata FROM dbo.inventario WHERE data_extracao >= ? AND data_extracao <= ? AND identificador_unico IS NOT NULL ORDER BY identificador_unico",
                metadataHasher
            );

            faturasResultado = comparar(ConstantesEntidades.FATURAS_POR_CLIENTE, faturasApi, faturasDb);
            inventarioResultado = comparar(ConstantesEntidades.INVENTARIO, inventarioApi, inventarioDb);
        } finally {
            GerenciadorConexao.fecharPool();
        }

        escreverArtefatos(rawDir, executionUuid, List.of(faturasResultado, inventarioResultado));

        final String mensagemFalha = construirResumoFalha(List.of(faturasResultado, inventarioResultado));
        assertTrue(faturasResultado.ok(), mensagemFalha);
        assertTrue(inventarioResultado.ok(), mensagemFalha);
    }

    private ApiState carregarFaturasPorCliente(final Path rawDir,
                                               final ValidacaoApiBanco24hDetalhadaMetadataHasher metadataHasher)
        throws IOException {
        final List<Path> arquivos = listarArquivosPaginados(rawDir, "faturas_por_cliente");
        final List<FaturaPorClienteDTO> dtos = carregarDtos(arquivos, new TypeReference<List<FaturaPorClienteDTO>>() { });
        final FaturaPorClienteMapper mapper = new FaturaPorClienteMapper();
        final List<FaturaPorClienteEntity> mapeadas = new ArrayList<>();
        final Map<String, Set<String>> hashesAceitosPorChave = new LinkedHashMap<>();
        int invalidos = 0;

        for (final FaturaPorClienteDTO dto : dtos) {
            try {
                final FaturaPorClienteEntity entity = mapper.toEntity(dto);
                if (entity == null || entity.getUniqueId() == null || entity.getUniqueId().isBlank()) {
                    invalidos++;
                    continue;
                }
                mapeadas.add(entity);
                hashesAceitosPorChave
                    .computeIfAbsent(entity.getUniqueId(), ignored -> new LinkedHashSet<>())
                    .add(metadataHasher.hashMetadata(ConstantesEntidades.FATURAS_POR_CLIENTE, entity.getMetadata()));
            } catch (final RuntimeException e) {
                invalidos++;
            }
        }

        final List<FaturaPorClienteEntity> deduplicadas = Deduplicator.deduplicarFaturasPorCliente(mapeadas);
        final Map<String, String> hashFinalPorChave = new LinkedHashMap<>();
        for (final FaturaPorClienteEntity entity : deduplicadas) {
            hashFinalPorChave.put(
                entity.getUniqueId(),
                metadataHasher.hashMetadata(ConstantesEntidades.FATURAS_POR_CLIENTE, entity.getMetadata())
            );
        }

        return new ApiState(
            ConstantesEntidades.FATURAS_POR_CLIENTE,
            dtos.size(),
            invalidos,
            hashFinalPorChave,
            hashesAceitosPorChave,
            arquivos
        );
    }

    private ApiState carregarInventario(final Path rawDir,
                                        final ValidacaoApiBanco24hDetalhadaMetadataHasher metadataHasher)
        throws IOException {
        final List<Path> arquivos = listarArquivosPaginados(rawDir, "inventario");
        final List<InventarioDTO> dtos = carregarDtos(arquivos, new TypeReference<List<InventarioDTO>>() { });
        final InventarioMapper mapper = new InventarioMapper();
        final List<InventarioEntity> mapeadas = new ArrayList<>();
        final Map<String, Set<String>> hashesAceitosPorChave = new LinkedHashMap<>();
        int invalidos = 0;

        for (final InventarioDTO dto : dtos) {
            try {
                final InventarioEntity entity = mapper.toEntity(dto);
                if (entity == null
                    || entity.getIdentificadorUnico() == null
                    || entity.getIdentificadorUnico().isBlank()) {
                    invalidos++;
                    continue;
                }
                mapeadas.add(entity);
                hashesAceitosPorChave
                    .computeIfAbsent(entity.getIdentificadorUnico(), ignored -> new LinkedHashSet<>())
                    .add(metadataHasher.hashMetadata(ConstantesEntidades.INVENTARIO, entity.getMetadata()));
            } catch (final RuntimeException e) {
                invalidos++;
            }
        }

        final Map<String, InventarioEntity> maisRecentes = new LinkedHashMap<>();
        for (final InventarioEntity entity : mapeadas) {
            final String chave = entity.getIdentificadorUnico();
            final InventarioEntity atual = maisRecentes.get(chave);
            if (atual == null || ehInventarioMaisRecente(entity, atual)) {
                maisRecentes.put(chave, entity);
            }
        }

        final Map<String, String> hashFinalPorChave = new LinkedHashMap<>();
        for (final InventarioEntity entity : maisRecentes.values()) {
            hashFinalPorChave.put(
                entity.getIdentificadorUnico(),
                metadataHasher.hashMetadata(ConstantesEntidades.INVENTARIO, entity.getMetadata())
            );
        }

        return new ApiState(
            ConstantesEntidades.INVENTARIO,
            dtos.size(),
            invalidos,
            hashFinalPorChave,
            hashesAceitosPorChave,
            arquivos
        );
    }

    private boolean ehInventarioMaisRecente(final InventarioEntity candidato, final InventarioEntity atual) {
        final OffsetDateTime candidatoFreshness =
            candidato.getPerformanceFinishedAt() != null ? candidato.getPerformanceFinishedAt()
                : candidato.getFinishedAt() != null ? candidato.getFinishedAt()
                : candidato.getStartedAt();
        final OffsetDateTime atualFreshness =
            atual.getPerformanceFinishedAt() != null ? atual.getPerformanceFinishedAt()
                : atual.getFinishedAt() != null ? atual.getFinishedAt()
                : atual.getStartedAt();

        if (candidatoFreshness == null) {
            return false;
        }
        if (atualFreshness == null) {
            return true;
        }
        return candidatoFreshness.isAfter(atualFreshness);
    }

    private <T> List<T> carregarDtos(final List<Path> arquivos, final TypeReference<List<T>> typeReference)
        throws IOException {
        final List<T> dtos = new ArrayList<>();
        for (final Path arquivo : arquivos) {
            dtos.addAll(MapperUtil.sharedJson().readValue(arquivo.toFile(), typeReference));
        }
        return dtos;
    }

    private List<Path> listarArquivosPaginados(final Path rawDir, final String entidade) throws IOException {
        try (var stream = Files.list(rawDir)) {
            final List<Path> arquivos = stream
                .filter(path -> path.getFileName().toString().startsWith(entidade + "-page"))
                .sorted(Comparator
                    .comparingInt(this::extrairNumeroPagina)
                    .thenComparing(path -> path.getFileName().toString()))
                .toList();
            if (arquivos.isEmpty()) {
                throw new IOException("Nenhum payload curl encontrado para entidade " + entidade + " em " + rawDir);
            }
            return arquivos;
        }
    }

    private int extrairNumeroPagina(final Path path) {
        final Matcher matcher = PAGE_FILE_PATTERN.matcher(path.getFileName().toString());
        if (!matcher.find()) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private DbState carregarEstadoBanco(final Connection conexao,
                                        final String executionUuid,
                                        final String entidade,
                                        final String sqlRegistros,
                                        final ValidacaoApiBanco24hDetalhadaMetadataHasher metadataHasher)
        throws SQLException {
        final AuditWindow janela = carregarJanelaExecucao(conexao, executionUuid, entidade);
        final Map<String, String> hashesPorChave = new LinkedHashMap<>();
        int linhas = 0;

        try (PreparedStatement stmt = conexao.prepareStatement(sqlRegistros)) {
            stmt.setTimestamp(1, Timestamp.valueOf(janela.startedAt()));
            stmt.setTimestamp(2, Timestamp.valueOf(janela.finishedAt()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    final String chave = normalizarChave(rs.getString("chave"));
                    if (chave == null) {
                        continue;
                    }
                    linhas++;
                    hashesPorChave.put(
                        chave,
                        metadataHasher.hashMetadata(entidade, rs.getString("metadata"))
                    );
                }
            }
        }

        return new DbState(entidade, janela, linhas, hashesPorChave);
    }

    private AuditWindow carregarJanelaExecucao(final Connection conexao,
                                               final String executionUuid,
                                               final String entidade)
        throws SQLException {
        final String sql = """
            SELECT TOP 1
                started_at,
                finished_at,
                janela_consulta_inicio,
                janela_consulta_fim,
                db_persistidos
            FROM dbo.sys_execution_audit
            WHERE execution_uuid = ?
              AND entidade = ?
              AND status_execucao IN ('COMPLETO', 'RECONCILIADO', 'RECONCILED')
              AND api_completa = 1
              AND started_at IS NOT NULL
              AND finished_at IS NOT NULL
            ORDER BY finished_at DESC
            """;

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, executionUuid);
            stmt.setString(2, entidade);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException(
                        "Janela de execution_audit nao encontrada para entidade " + entidade
                            + " na execucao " + executionUuid
                    );
                }
                return new AuditWindow(
                    rs.getTimestamp("started_at").toLocalDateTime(),
                    rs.getTimestamp("finished_at").toLocalDateTime(),
                    rs.getTimestamp("janela_consulta_inicio").toLocalDateTime(),
                    rs.getTimestamp("janela_consulta_fim").toLocalDateTime(),
                    rs.getInt("db_persistidos")
                );
            }
        }
    }

    private ComparisonResult comparar(final String entidade, final ApiState api, final DbState db) {
        final Set<String> faltantesNoBanco = new HashSet<>(api.hashFinalPorChave().keySet());
        faltantesNoBanco.removeAll(db.hashPorChave().keySet());

        final Set<String> somenteNoBanco = new HashSet<>(db.hashPorChave().keySet());
        somenteNoBanco.removeAll(api.hashFinalPorChave().keySet());

        final List<String> divergenciasDados = new ArrayList<>();
        final List<String> toleradasPorDuplicidade = new ArrayList<>();
        for (final Map.Entry<String, String> entry : api.hashFinalPorChave().entrySet()) {
            final String chave = entry.getKey();
            final String dbHash = db.hashPorChave().get(chave);
            if (dbHash == null) {
                continue;
            }

            if (Objects.equals(entry.getValue(), dbHash)) {
                continue;
            }

            final Set<String> aceitos = api.hashesAceitosPorChave().getOrDefault(chave, Set.of());
            if (aceitos.contains(dbHash)) {
                toleradasPorDuplicidade.add(chave);
            } else {
                divergenciasDados.add(chave);
            }
        }

        return new ComparisonResult(
            entidade,
            api.rawCount(),
            api.uniqueCount(),
            api.invalidCount(),
            db.rowCount(),
            db.uniqueCount(),
            db.janela(),
            ordenar(faltantesNoBanco),
            ordenar(somenteNoBanco),
            ordenar(divergenciasDados),
            ordenar(toleradasPorDuplicidade),
            api.rawFiles().stream().map(path -> path.getFileName().toString()).toList()
        );
    }

    private List<String> ordenar(final Set<String> valores) {
        return valores.stream().sorted().toList();
    }

    private List<String> ordenar(final List<String> valores) {
        return valores.stream().sorted().toList();
    }

    private String normalizarChave(final String valor) {
        if (valor == null) {
            return null;
        }
        final String texto = valor.trim();
        return texto.isEmpty() ? null : texto;
    }

    private void escreverArtefatos(final Path rawDir,
                                   final String executionUuid,
                                   final List<ComparisonResult> resultados)
        throws IOException {
        final Path resultsDir = Paths.get("src", "test", "results");
        Files.createDirectories(resultsDir);

        final String sufixo = LocalDateTime.now().format(REPORT_TS);
        final Path jsonPath = resultsDir.resolve("curl-java-vs-banco-periodo-fechado-" + sufixo + ".json");
        final Path mdPath = resultsDir.resolve("curl-java-vs-banco-periodo-fechado-" + sufixo + ".md");

        final Map<String, Object> report = new LinkedHashMap<>();
        report.put("executado_em", LocalDateTime.now().toString());
        report.put("execution_uuid", executionUuid);
        report.put("raw_dir", rawDir.toAbsolutePath().toString());
        report.put("resultados", resultados);

        Files.writeString(
            jsonPath,
            MapperUtil.sharedJson().writerWithDefaultPrettyPrinter().writeValueAsString(report),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        );

        final StringBuilder markdown = new StringBuilder();
        markdown.append("# Prova operacional CURL x banco").append(System.lineSeparator()).append(System.lineSeparator());
        markdown.append("- execution_uuid: `").append(executionUuid).append('`').append(System.lineSeparator());
        markdown.append("- raw_dir: `").append(rawDir.toAbsolutePath()).append('`').append(System.lineSeparator());
        markdown.append(System.lineSeparator());

        for (final ComparisonResult resultado : resultados) {
            markdown.append("## ").append(resultado.entidade()).append(System.lineSeparator()).append(System.lineSeparator());
            markdown.append("- ok: ").append(resultado.ok()).append(System.lineSeparator());
            markdown.append("- api_raw: ").append(resultado.apiRaw()).append(System.lineSeparator());
            markdown.append("- api_unico: ").append(resultado.apiUnico()).append(System.lineSeparator());
            markdown.append("- api_invalidos: ").append(resultado.apiInvalidos()).append(System.lineSeparator());
            markdown.append("- banco_linhas: ").append(resultado.bancoLinhas()).append(System.lineSeparator());
            markdown.append("- banco_unico: ").append(resultado.bancoUnico()).append(System.lineSeparator());
            markdown.append("- db_persistidos: ").append(resultado.janela().dbPersistidos()).append(System.lineSeparator());
            markdown.append("- started_at: `").append(resultado.janela().startedAt()).append('`').append(System.lineSeparator());
            markdown.append("- finished_at: `").append(resultado.janela().finishedAt()).append('`').append(System.lineSeparator());
            markdown.append("- faltantes_no_banco: ").append(resultado.faltantesNoBanco().size()).append(System.lineSeparator());
            markdown.append("- somente_no_banco: ").append(resultado.somenteNoBanco().size()).append(System.lineSeparator());
            markdown.append("- divergencias_dados: ").append(resultado.divergenciasDados().size()).append(System.lineSeparator());
            markdown.append("- toleradas_por_duplicidade: ").append(resultado.toleradasPorDuplicidade().size()).append(System.lineSeparator());
            markdown.append("- arquivos_raw: ").append(String.join(", ", resultado.arquivosRaw())).append(System.lineSeparator());
            markdown.append(System.lineSeparator());

            if (!resultado.faltantesNoBanco().isEmpty()) {
                markdown.append("faltantes_no_banco: `")
                    .append(String.join("`, `", limitar(resultado.faltantesNoBanco(), 20)))
                    .append('`')
                    .append(System.lineSeparator());
            }
            if (!resultado.somenteNoBanco().isEmpty()) {
                markdown.append("somente_no_banco: `")
                    .append(String.join("`, `", limitar(resultado.somenteNoBanco(), 20)))
                    .append('`')
                    .append(System.lineSeparator());
            }
            if (!resultado.divergenciasDados().isEmpty()) {
                markdown.append("divergencias_dados: `")
                    .append(String.join("`, `", limitar(resultado.divergenciasDados(), 20)))
                    .append('`')
                    .append(System.lineSeparator());
            }
            if (!resultado.toleradasPorDuplicidade().isEmpty()) {
                markdown.append("toleradas_por_duplicidade: `")
                    .append(String.join("`, `", limitar(resultado.toleradasPorDuplicidade(), 20)))
                    .append('`')
                    .append(System.lineSeparator());
            }
            markdown.append(System.lineSeparator());
        }

        Files.writeString(
            mdPath,
            markdown.toString(),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        );
    }

    private List<String> limitar(final List<String> valores, final int limite) {
        if (valores.size() <= limite) {
            return valores;
        }
        return valores.subList(0, limite);
    }

    private String construirResumoFalha(final List<ComparisonResult> resultados) {
        final StringBuilder sb = new StringBuilder("Prova operacional CURL x banco falhou:");
        for (final ComparisonResult resultado : resultados) {
            sb.append(System.lineSeparator())
                .append("- ")
                .append(resultado.entidade())
                .append(" | faltantes=")
                .append(resultado.faltantesNoBanco().size())
                .append(" | somente_banco=")
                .append(resultado.somenteNoBanco().size())
                .append(" | divergencias_dados=")
                .append(resultado.divergenciasDados().size())
                .append(" | toleradas_duplicidade=")
                .append(resultado.toleradasPorDuplicidade().size());
        }
        return sb.toString();
    }

    private record ApiState(
        String entidade,
        int rawCount,
        int invalidCount,
        Map<String, String> hashFinalPorChave,
        Map<String, Set<String>> hashesAceitosPorChave,
        List<Path> rawFiles
    ) {
        int uniqueCount() {
            return hashFinalPorChave.size();
        }
    }

    private record AuditWindow(
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime janelaConsultaInicio,
        LocalDateTime janelaConsultaFim,
        int dbPersistidos
    ) { }

    private record DbState(
        String entidade,
        AuditWindow janela,
        int rowCount,
        Map<String, String> hashPorChave
    ) {
        int uniqueCount() {
            return hashPorChave.size();
        }
    }

    private record ComparisonResult(
        String entidade,
        int apiRaw,
        int apiUnico,
        int apiInvalidos,
        int bancoLinhas,
        int bancoUnico,
        AuditWindow janela,
        List<String> faltantesNoBanco,
        List<String> somenteNoBanco,
        List<String> divergenciasDados,
        List<String> toleradasPorDuplicidade,
        List<String> arquivosRaw
    ) {
        boolean ok() {
            return faltantesNoBanco.isEmpty()
                && somenteNoBanco.isEmpty()
                && divergenciasDados.isEmpty();
        }
    }
}
