package br.com.extrator.aplicacao.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.contexto.AplicacaoContexto;
import br.com.extrator.aplicacao.portas.ManifestoOrfaoQueryPort;
import br.com.extrator.suporte.configuracao.ConfigEtl;

class PreBackfillReferencialColetasUseCaseTest {

    @Test
    void deveAplicarBufferAoOrfaoMaisAntigo() throws Exception {
        final Optional<LocalDate> dataOrfao = Optional.of(LocalDate.of(2026, 3, 6));
        AplicacaoContexto.registrar((ManifestoOrfaoQueryPort) () -> dataOrfao);

        final LocalDate resultado = resolverInicioEfetivo(LocalDate.of(2026, 3, 9), dataOrfao);

        assertEquals(
            LocalDate.of(2026, 3, 6).minusDays(ConfigEtl.obterEtlReferencialColetasBackfillBufferDias()),
            resultado
        );
    }

    @Test
    void deveManterInicioEstaticoQuandoNaoExistemOrfaos() throws Exception {
        final Optional<LocalDate> dataOrfao = Optional.empty();
        AplicacaoContexto.registrar((ManifestoOrfaoQueryPort) () -> dataOrfao);

        final LocalDate resultado = resolverInicioEfetivo(LocalDate.of(2026, 3, 9), dataOrfao);

        assertEquals(LocalDate.of(2026, 3, 9), resultado);
    }

    @Test
    void deveManterInicioEstaticoQuandoBufferJaEstaCoberto() throws Exception {
        final Optional<LocalDate> dataOrfao = Optional.of(LocalDate.of(2026, 3, 16));
        AplicacaoContexto.registrar((ManifestoOrfaoQueryPort) () -> dataOrfao);

        final LocalDate resultado = resolverInicioEfetivo(LocalDate.of(2026, 3, 9), dataOrfao);

        assertEquals(LocalDate.of(2026, 3, 9), resultado);
    }

    @Test
    void deveLimitarExpansaoDinamicaDoPreBackfill() throws Exception {
        final String anterior = System.getProperty("ETL_REFERENCIAL_COLETAS_BACKFILL_MAX_EXPANSAO_DIAS");
        System.setProperty("ETL_REFERENCIAL_COLETAS_BACKFILL_MAX_EXPANSAO_DIAS", "30");
        try {
            final Optional<LocalDate> dataOrfao = Optional.of(LocalDate.of(2025, 9, 30));
            AplicacaoContexto.registrar((ManifestoOrfaoQueryPort) () -> dataOrfao);

            final LocalDate inicioEstatico = LocalDate.of(2026, 3, 25);
            final LocalDate resultado = resolverInicioEfetivo(inicioEstatico, dataOrfao);

            assertEquals(inicioEstatico.minusDays(30), resultado);
        } finally {
            restaurarPropriedade("ETL_REFERENCIAL_COLETAS_BACKFILL_MAX_EXPANSAO_DIAS", anterior);
        }
    }

    @Test
    void deveEncerrarHidratacaoRetroativaNoDiaAnteriorAoPeriodoPrincipal() throws Exception {
        assertEquals(
            LocalDate.of(2026, 3, 8),
            resolverFimRetroativoPosExtracao(LocalDate.of(2026, 3, 9))
        );
    }

    @Test
    void deveIniciarLookaheadApenasAposOFimDoPeriodoPrincipal() throws Exception {
        assertEquals(
            LocalDate.of(2026, 3, 10),
            resolverInicioLookaheadPosExtracao(LocalDate.of(2026, 3, 9))
        );
    }

    @Test
    void deveAplicarLookaheadSomenteNoPeriodoPosterior() throws Exception {
        final Optional<LocalDate> resultado = resolverFimLookaheadPosExtracao(LocalDate.of(2026, 3, 9));

        assertEquals(
            Optional.of(LocalDate.of(2026, 3, 9).plusDays(ConfigEtl.obterEtlReferencialColetasLookaheadDias())),
            resultado
        );
    }

    private LocalDate resolverInicioEfetivo(
        final LocalDate inicioEstatico,
        final Optional<LocalDate> dataOrfao
    ) throws Exception {
        final PreBackfillReferencialColetasUseCase useCase = new PreBackfillReferencialColetasUseCase();
        final Method metodo = PreBackfillReferencialColetasUseCase.class
            .getDeclaredMethod("resolverInicioEfetivo", LocalDate.class, Optional.class);
        metodo.setAccessible(true);
        return (LocalDate) metodo.invoke(useCase, inicioEstatico, dataOrfao);
    }

    private LocalDate resolverFimRetroativoPosExtracao(final LocalDate inicioPrincipal) throws Exception {
        final PreBackfillReferencialColetasUseCase useCase = new PreBackfillReferencialColetasUseCase();
        final Method metodo = PreBackfillReferencialColetasUseCase.class
            .getDeclaredMethod("resolverFimRetroativoPosExtracao", LocalDate.class);
        metodo.setAccessible(true);
        return (LocalDate) metodo.invoke(useCase, inicioPrincipal);
    }

    private LocalDate resolverInicioLookaheadPosExtracao(final LocalDate fimPrincipal) throws Exception {
        final PreBackfillReferencialColetasUseCase useCase = new PreBackfillReferencialColetasUseCase();
        final Method metodo = PreBackfillReferencialColetasUseCase.class
            .getDeclaredMethod("resolverInicioLookaheadPosExtracao", LocalDate.class);
        metodo.setAccessible(true);
        return (LocalDate) metodo.invoke(useCase, fimPrincipal);
    }

    @SuppressWarnings("unchecked")
    private Optional<LocalDate> resolverFimLookaheadPosExtracao(final LocalDate fimPrincipal) throws Exception {
        final PreBackfillReferencialColetasUseCase useCase = new PreBackfillReferencialColetasUseCase();
        final Method metodo = PreBackfillReferencialColetasUseCase.class
            .getDeclaredMethod("resolverFimLookaheadPosExtracao", LocalDate.class);
        metodo.setAccessible(true);
        return (Optional<LocalDate>) metodo.invoke(useCase, fimPrincipal);
    }

    private void restaurarPropriedade(final String chave, final String valorAnterior) {
        if (valorAnterior == null) {
            System.clearProperty(chave);
            return;
        }
        System.setProperty(chave, valorAnterior);
    }
}
