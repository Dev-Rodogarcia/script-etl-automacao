package br.com.extrator.analises.indicadoresgestao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class IndicadoresGestaoRulesPrototypeTest {

    @Test
    void indicador1DeveClassificarPerformanceConformeRegraHomologadaPeloGestor() {
        final PerformanceEntrega adiantado =
            PerformanceEntrega.calcular(LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 3));
        final PerformanceEntrega noDia =
            PerformanceEntrega.calcular(LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 5));
        final PerformanceEntrega atrasado =
            PerformanceEntrega.calcular(LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 7));

        assertEquals(-2L, adiantado.performanceEmDias());
        assertEquals(StatusPrazo.NO_PRAZO, adiantado.status());

        assertEquals(0L, noDia.performanceEmDias());
        assertEquals(StatusPrazo.NO_PRAZO, noDia.status());

        assertEquals(2L, atrasado.performanceEmDias());
        assertEquals(StatusPrazo.FORA_DO_PRAZO, atrasado.status());
    }

    @Test
    void indicador2DeveSomarEmitidosEDescarregamentosAteChegarAoTotalOficial() {
        final String filialSpo = "SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA";

        final List<ManifestoCandidato> manifestos = new ArrayList<>();
        IntStream.rangeClosed(1, 120)
            .forEach(i -> manifestos.add(new ManifestoCandidato("EMI-" + i, filialSpo, List.of())));
        IntStream.rangeClosed(1, 35)
            .forEach(i -> manifestos.add(new ManifestoCandidato(
                "DES-" + i,
                "CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
                List.of(filialSpo, filialSpo)
            )));

        final DenominadorManifestos resultado =
            DenominadorManifestos.calcular(filialSpo, manifestos, Map.of());

        assertEquals(120, resultado.emitidos());
        assertEquals(35, resultado.descarregados());
        assertEquals(155, resultado.total());
    }

    @Test
    void indicador2DeveDeduplicarRepeticoesDoMesmoManifestoNoDescarregamento() {
        final String filialCwb = "CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA";
        final List<ManifestoCandidato> manifestos = List.of(
            new ManifestoCandidato(
                "M-1",
                "SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
                List.of(filialCwb, filialCwb, filialCwb)
            )
        );

        final DenominadorManifestos resultado =
            DenominadorManifestos.calcular(filialCwb, manifestos, Map.of());

        assertEquals(0, resultado.emitidos());
        assertEquals(1, resultado.descarregados());
        assertEquals(1, resultado.total());
    }

    @Test
    void indicador2DevePermitirNormalizarParceirosParaFilialCanonica() {
        final String filialCwb = "CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA";
        final String parceiroPatoBranco = "PTO - NEW TRANSPORTES | PARCEIRO PATO BRANCO";
        final List<ManifestoCandidato> manifestos = List.of(
            new ManifestoCandidato(
                "M-2",
                "CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
                List.of(parceiroPatoBranco)
            )
        );

        final DenominadorManifestos semDicionario =
            DenominadorManifestos.calcular(filialCwb, manifestos, Map.of());
        final DenominadorManifestos comDicionario =
            DenominadorManifestos.calcular(filialCwb, manifestos, Map.of(parceiroPatoBranco, filialCwb));

        assertEquals(0, semDicionario.descarregados());
        assertEquals(1, comDicionario.descarregados());
    }

    @Test
    void indicador3DeveSepararKpiDeCubagemDaQualidadeDoPesoReal() {
        final AvaliacaoCubagem cubadoSemPesoReal =
            AvaliacaoCubagem.avaliar(new BigDecimal("1.25"), BigDecimal.ZERO, BigDecimal.ZERO);
        final AvaliacaoCubagem naoCubadoComPesoReal =
            AvaliacaoCubagem.avaliar(BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("15.00"));

        assertTrue(cubadoSemPesoReal.cubado());
        assertFalse(cubadoSemPesoReal.qualidadePesoRealOk());

        assertFalse(naoCubadoComPesoReal.cubado());
        assertTrue(naoCubadoComPesoReal.qualidadePesoRealOk());
    }

    @Test
    void indicador4DeveConsolidarValorAbsolutoGerencialESinalOriginalAnalitico() {
        final List<SinistroLinha> linhas = List.of(
            new SinistroLinha("1001", new BigDecimal("-120.50")),
            new SinistroLinha("1001", new BigDecimal("-120.50")),
            new SinistroLinha("1002", BigDecimal.ZERO),
            new SinistroLinha("1003", new BigDecimal("25.00"))
        );

        final ResumoIndenizacao resumo = ResumoIndenizacao.calcular(linhas);

        assertEquals(3, resumo.sinistrosUnicos());
        assertEquals(new BigDecimal("145.50"), resumo.valorGerencialAbsoluto());
        assertEquals(new BigDecimal("-95.50"), resumo.valorAnaliticoOriginal());
    }

    @Test
    void cotacaoDeveConsiderarConversaoPorCteOuNfse() {
        final List<CotacaoCandidata> cotacoes = List.of(
            new CotacaoCandidata("1", new BigDecimal("100.00"), true, false, "FRACIONADA"),
            new CotacaoCandidata("2", new BigDecimal("50.00"), false, true, "FRACIONADA"),
            new CotacaoCandidata("3", new BigDecimal("30.00"), false, false, "FECHADA"),
            new CotacaoCandidata("4", new BigDecimal("20.00"), false, false, "FRACIONADA")
        );

        final ResumoCotacao resumo = ResumoCotacao.calcular(cotacoes);

        assertEquals(4, resumo.quantidadeTotal());
        assertEquals(2, resumo.quantidadeConvertida());
        assertEquals(new BigDecimal("200.00"), resumo.valorTotalCotado());
        assertEquals(new BigDecimal("150.00"), resumo.valorTotalConvertido());
        assertEquals(0.5d, resumo.taxaConversaoQuantidade(), 0.0001d);
        assertEquals(0.75d, resumo.taxaConversaoValor(), 0.0001d);
    }

    private enum StatusPrazo {
        NO_PRAZO,
        FORA_DO_PRAZO
    }

    private record PerformanceEntrega(long performanceEmDias, StatusPrazo status) {
        private static PerformanceEntrega calcular(final LocalDate previsaoEntrega, final LocalDate dataFinalizacao) {
            final long performanceEmDias = ChronoUnit.DAYS.between(previsaoEntrega, dataFinalizacao);
            final StatusPrazo status = performanceEmDias <= 0 ? StatusPrazo.NO_PRAZO : StatusPrazo.FORA_DO_PRAZO;
            return new PerformanceEntrega(performanceEmDias, status);
        }
    }

    private record ManifestoCandidato(String numero, String filialEmitente, List<String> descarregamentos) {
    }

    private record DenominadorManifestos(int emitidos, int descarregados) {
        private int total() {
            return emitidos + descarregados;
        }

        private static DenominadorManifestos calcular(
            final String filialAlvo,
            final List<ManifestoCandidato> manifestos,
            final Map<String, String> dicionarioDescarregamento
        ) {
            final int emitidos = (int) manifestos.stream()
                .filter(manifesto -> filialAlvo.equals(manifesto.filialEmitente()))
                .map(ManifestoCandidato::numero)
                .distinct()
                .count();

            final int descarregados = (int) manifestos.stream()
                .flatMap(manifesto -> normalizarDescarregamentos(manifesto, dicionarioDescarregamento).stream()
                    .map(filialNormalizada -> manifesto.numero() + "|" + filialNormalizada))
                .filter(chave -> chave.endsWith("|" + filialAlvo))
                .distinct()
                .count();

            return new DenominadorManifestos(emitidos, descarregados);
        }

        private static Set<String> normalizarDescarregamentos(
            final ManifestoCandidato manifesto,
            final Map<String, String> dicionarioDescarregamento
        ) {
            return manifesto.descarregamentos().stream()
                .map(String::trim)
                .filter(valor -> !valor.isBlank())
                .map(valor -> dicionarioDescarregamento.getOrDefault(valor, valor))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    private record AvaliacaoCubagem(boolean cubado, boolean qualidadePesoRealOk) {
        private static AvaliacaoCubagem avaliar(
            final BigDecimal totalM3,
            final BigDecimal pesoCubado,
            final BigDecimal pesoReal
        ) {
            final boolean cubado = maiorQueZero(totalM3) || maiorQueZero(pesoCubado);
            final boolean qualidadePesoRealOk = maiorQueZero(pesoReal);
            return new AvaliacaoCubagem(cubado, qualidadePesoRealOk);
        }
    }

    private record SinistroLinha(String sequenceCode, BigDecimal valor) {
    }

    private record ResumoIndenizacao(int sinistrosUnicos, BigDecimal valorGerencialAbsoluto, BigDecimal valorAnaliticoOriginal) {
        private static ResumoIndenizacao calcular(final List<SinistroLinha> linhas) {
            final Map<String, BigDecimal> unicos = new LinkedHashMap<>();
            for (final SinistroLinha linha : linhas) {
                unicos.putIfAbsent(linha.sequenceCode(), linha.valor());
            }

            BigDecimal valorGerencialAbsoluto = BigDecimal.ZERO;
            BigDecimal valorAnaliticoOriginal = BigDecimal.ZERO;

            for (final BigDecimal valor : unicos.values()) {
                valorGerencialAbsoluto = valorGerencialAbsoluto.add(valor.abs());
                valorAnaliticoOriginal = valorAnaliticoOriginal.add(valor);
            }

            return new ResumoIndenizacao(unicos.size(), valorGerencialAbsoluto, valorAnaliticoOriginal);
        }
    }

    private record CotacaoCandidata(String sequenceCode, BigDecimal valorTotal, boolean temCteEmitido, boolean temNfseEmitida, String tipoOperacao) {
        private boolean convertida() {
            return temCteEmitido || temNfseEmitida;
        }
    }

    private record ResumoCotacao(
        int quantidadeTotal,
        int quantidadeConvertida,
        BigDecimal valorTotalCotado,
        BigDecimal valorTotalConvertido,
        double taxaConversaoQuantidade,
        double taxaConversaoValor
    ) {
        private static ResumoCotacao calcular(final List<CotacaoCandidata> cotacoes) {
            final int quantidadeTotal = cotacoes.size();
            final int quantidadeConvertida = (int) cotacoes.stream().filter(CotacaoCandidata::convertida).count();

            final BigDecimal valorTotalCotado = cotacoes.stream()
                .map(CotacaoCandidata::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            final BigDecimal valorTotalConvertido = cotacoes.stream()
                .filter(CotacaoCandidata::convertida)
                .map(CotacaoCandidata::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            final double taxaConversaoQuantidade =
                quantidadeTotal == 0 ? 0.0d : (double) quantidadeConvertida / quantidadeTotal;
            final double taxaConversaoValor =
                BigDecimal.ZERO.compareTo(valorTotalCotado) == 0
                    ? 0.0d
                    : valorTotalConvertido.divide(valorTotalCotado, 8, java.math.RoundingMode.HALF_UP).doubleValue();

            return new ResumoCotacao(
                quantidadeTotal,
                quantidadeConvertida,
                valorTotalCotado,
                valorTotalConvertido,
                taxaConversaoQuantidade,
                taxaConversaoValor
            );
        }
    }

    private static boolean maiorQueZero(final BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }
}
