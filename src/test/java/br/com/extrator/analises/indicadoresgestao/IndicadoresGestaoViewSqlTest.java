package br.com.extrator.analises.indicadoresgestao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class IndicadoresGestaoViewSqlTest {
    private static final Pattern MOJIBAKE_PATTERN = Pattern.compile(
        "\\x{00C3}[\\x{0080}-\\x{00BF}\\x{0192}\\x{201A}\\x{00A2}]|"
            + "\\x{00C2}[\\x{0080}-\\x{00BF}]|\\x{FFFD}"
    );

    @Test
    void fretesPowerBiDeveExporColunasOficiaisDePerformanceECubagem() throws IOException {
        final String sql = lerSql("database/views/012_criar_view_fretes_powerbi.sql");

        assertContem(sql, "[Nº Minuta]");
        assertContem(sql, "[Filial Emissora]");
        assertContem(sql, "[Responsável pela Região de Destino]");
        assertContem(sql, "[Data de Finalização]");
        assertContem(sql, "[Finalização da Performance]");
        assertContem(sql, "[Performance Diferença de Dias]");
        assertContem(sql, "[Performance Status]");
        assertContem(sql, "[Performance Status Dif de Dias]");
        assertContem(sql, "[Performance Status Dif de Dias Oficial]");
        assertContem(sql, "[Peso Real]");
        assertContem(sql, "[Peso Cubado]");
        assertContem(sql, "[Total M3]");
        assertContem(sql, "[Cortesia]");
        assertContem(sql, "[Cortesia Flag]");
        assertContem(sql, "NULLIF(LTRIM(RTRIM(f.nfse_series)), '')");
        assertContem(sql, "LEFT JOIN dbo.localizacao_cargas");
    }

    @Test
    void fretesPowerBiDeveUsarFinishedAtComoFallbackDaFinalizacaoDePerformance() throws IOException {
        final String sql = lerSql("database/views/012_criar_view_fretes_powerbi.sql");

        assertContem(sql, "COALESCE(f.fit_dpn_performance_finished_at, f.finished_at) AS finalizacao_performance_oficial");
    }

    @Test
    void coletasPowerBiDeveExporSolicitacaoComoDataNativaEIndiceDashboard() throws IOException {
        final String tabelaSql = lerSql("database/tabelas/001_criar_tabela_coletas.sql");
        final String viewSql = lerSql("database/views/013_criar_view_coletas_powerbi.sql");
        final String indicesSql = lerSql("database/indices/001_criar_indices_performance.sql");
        final String migrationSql = lerSql("database/migrations/018_adicionar_indice_coletas_request_date_dashboard.sql");

        assertContem(tabelaSql, "request_date DATE");
        assertContem(viewSql, "c.request_date AS [Solicitacao]");
        assertContem(indicesSql, "IX_coletas_request_date_dashboard");
        assertContem(indicesSql, "ON dbo.coletas(request_date, status, pick_region, cidade_coleta)");
        assertContem(migrationSql, "IX_coletas_request_date_dashboard");
        assertContem(migrationSql, "ON dbo.coletas(request_date, status, pick_region, cidade_coleta)");
        assertSemMojibake(viewSql, "database/views/013_criar_view_coletas_powerbi.sql");
        assertSemMojibake(migrationSql, "database/migrations/018_adicionar_indice_coletas_request_date_dashboard.sql");
    }

    @Test
    void fretesPowerBiDeveExporFaturamentoMaterializadoSemRegraPesadaNaView() throws IOException {
        final String sql = lerSql("database/views/012_criar_view_fretes_powerbi.sql");

        assertContem(sql, "f.data_referencia_faturamento AS data_referencia_faturamento");
        assertContem(sql, "f.is_elegivel_faturamento AS is_elegivel_faturamento");
        assertFalse(
            sql.contains("%bloqueio%") || sql.contains("%anulacao%") || sql.contains("%isolamento%"),
            "A view de fretes deve apenas expor os campos materializados pelo ETL, sem regra pesada de string."
        );
        assertSemMojibake(sql, "database/views/012_criar_view_fretes_powerbi.sql");
    }

    @Test
    void migrationFretesFaturamentoDeveMaterializarRegraEIndice() throws IOException {
        final String sql = lerSql("database/migrations/016_materializar_faturamento_fretes.sql");

        assertContem(sql, "ALTER TABLE dbo.fretes ADD data_referencia_faturamento DATETIMEOFFSET NULL");
        assertContem(sql, "ALTER TABLE dbo.fretes ADD is_elegivel_faturamento BIT NULL");
        assertContem(sql, "COALESCE(f.cte_issued_at, f.servico_em) AS data_referencia_faturamento");
        assertContem(sql, "WHEN f.cortesia = 1 THEN 0");
        assertContem(sql, "COLLATE Latin1_General_CI_AI LIKE N''%bloqueio%''");
        assertContem(sql, "COLLATE Latin1_General_CI_AI LIKE N''%anulacao%''");
        assertContem(sql, "COLLATE Latin1_General_CI_AI LIKE N''%isolamento%''");
        assertContem(sql, "IX_fretes_faturamento_data_elegivel");
        assertSemMojibake(sql, "database/migrations/016_materializar_faturamento_fretes.sql");
    }

    @Test
    void manifestosPowerBiDeveExporLocalDeDescarregamentoComNomeDoNegocio() throws IOException {
        final String sql = lerSql("database/views/018_criar_view_manifestos_powerbi.sql");

        assertContem(sql, "[Filial Emissora]");
        assertContem(sql, "[Local de Descarregamento]");
    }

    @Test
    void inventarioPowerBiDeveExporFilialEmissoraDoFrete() throws IOException {
        final String sql = lerSql("database/views/020_criar_view_inventario_powerbi.sql");

        assertContem(sql, "[Filial da Ordem de Conferência]");
        assertContem(sql, "[Filial Emissora do Frete]");
        assertContem(sql, "[Data de Finalização]");
        assertContem(sql, "CheckIn::Order::Return");
        assertContem(sql, "'Retorno'");
        assertContem(sql, "LEFT JOIN dbo.fretes");
    }

    @Test
    void localizacaoCargasPowerBiDeveExporResponsavelPelaRegiaoDeDestino() throws IOException {
        final String sql = lerSql("database/views/017_criar_view_localizacao_cargas_powerbi.sql");

        assertContem(sql, "[Responsável pela Região de Destino]");
    }

    @Test
    void rasterTransitTimeDeveExporDataDeExtracaoParaDashboardHorariosCorte() throws IOException {
        final String sql = lerSql("database/views/022_criar_view_raster_sm_transit_time.sql");

        assertContem(sql, "v.data_extracao AS viagem_data_extracao");
        assertContem(sql, "p.data_extracao AS parada_data_extracao");
        assertContem(sql, "END AS data_extracao_raster");
        assertContem(sql, "data_extracao_raster AS [Data de extracao]");
    }

    @Test
    void rasterTransitTimeDeveExporCamposConsumidosPeloDashboardSemMojibake() throws IOException {
        final String sql = lerSql("database/views/022_criar_view_raster_sm_transit_time.sql");

        assertContem(sql, "origem_sm AS [ORIGEM - SM]");
        assertContem(sql, "destino_sm AS [DESTINO - SM]");
        assertContem(sql, "origem_destino AS [Origem x Destino]");
        assertContem(sql, "origem_nome AS [ORIGEM]");
        assertContem(sql, "ordem_parada_label AS [ORDEM]");
        assertContem(sql, "destino_nome AS [DESTINO]");
        assertContem(sql, "horario_corte_texto AS [HORÁRIO CORTE]");
        assertContem(sql, "previsao_chegada_destino AS [PREV. CHEGADA (destino)]");
        assertContem(sql, "transit_time_texto AS [TRANSIT TIME]");
        assertContem(sql, "origem_sm");
        assertContem(sql, "destino_sm");
        assertContem(sql, "origem_destino");
        assertContem(sql, "horario_corte_texto");
        assertContem(sql, "previsao_chegada_destino");
        assertContem(sql, "transit_time_texto");
        assertSemMojibake(sql, "database/views/022_criar_view_raster_sm_transit_time.sql");
    }

    private String lerSql(final String caminhoRelativo) throws IOException {
        return Files.readString(Path.of(caminhoRelativo), StandardCharsets.UTF_8);
    }

    private void assertContem(final String sql, final String trechoEsperado) {
        assertTrue(
            sql.contains(trechoEsperado),
            "Esperado encontrar o trecho '" + trechoEsperado + "' no SQL da view."
        );
    }

    private void assertSemMojibake(final String conteudo, final String nomeArquivo) {
        assertFalse(
            MOJIBAKE_PATTERN.matcher(conteudo).find(),
            "Mojibake detectado em " + nomeArquivo + ". Corrija o arquivo em UTF-8 antes de seguir."
        );
    }
}
