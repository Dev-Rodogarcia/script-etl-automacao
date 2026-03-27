package br.com.extrator.aplicacao.extracao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.contexto.AplicacaoContexto;
import br.com.extrator.aplicacao.pipeline.PipelineStep;
import br.com.extrator.aplicacao.pipeline.runtime.StepExecutionResult;
import br.com.extrator.aplicacao.portas.DataExportGateway;
import br.com.extrator.aplicacao.portas.GraphQLGateway;

class PlanejadorEscopoExtracaoIntervaloTest {

    private Object gatewayGraphqlAnterior;
    private Object gatewayDataExportAnterior;

    @BeforeEach
    void prepararContexto() throws Exception {
        gatewayGraphqlAnterior = lerCampoContexto("graphQLGateway");
        gatewayDataExportAnterior = lerCampoContexto("dataExportGateway");
        AplicacaoContexto.registrar((GraphQLGateway) (dataInicio, dataFim, entidade) ->
            StepExecutionResult.builder("graphql:" + entidade, entidade).build()
        );
        AplicacaoContexto.registrar((DataExportGateway) (dataInicio, dataFim, entidade) ->
            StepExecutionResult.builder("dataexport:" + entidade, entidade).build()
        );
    }

    @AfterEach
    void restaurarContexto() throws Exception {
        escreverCampoContexto("graphQLGateway", gatewayGraphqlAnterior);
        escreverCampoContexto("dataExportGateway", gatewayDataExportAnterior);
    }

    @Test
    void deveCriarStepsGranularesParaEscopoCompletoSemFaturasGraphql() {
        final PlanejadorEscopoExtracaoIntervalo planejador = new PlanejadorEscopoExtracaoIntervalo();

        final List<String> steps = planejador.criarSteps(null, null, false)
            .stream()
            .map(PipelineStep::obterNomeEtapa)
            .toList();

        assertEquals(
            List.of(
                "graphql:usuarios_sistema",
                "graphql:coletas",
                "graphql:fretes",
                "dataexport:manifestos",
                "dataexport:cotacoes",
                "dataexport:localizacao_cargas",
                "dataexport:contas_a_pagar",
                "dataexport:faturas_por_cliente"
            ),
            steps
        );
    }

    @Test
    void deveCriarStepsGranularesParaApiGraphqlComFaturasQuandoNaoHouverEntidadeEspecifica() {
        final PlanejadorEscopoExtracaoIntervalo planejador = new PlanejadorEscopoExtracaoIntervalo();

        final List<String> steps = planejador.criarSteps("graphql", null, true)
            .stream()
            .map(PipelineStep::obterNomeEtapa)
            .toList();

        assertEquals(
            List.of(
                "graphql:usuarios_sistema",
                "graphql:coletas",
                "graphql:fretes",
                "graphql:faturas_graphql"
            ),
            steps
        );
    }

    private Object lerCampoContexto(final String nomeCampo) throws Exception {
        final Field campo = AplicacaoContexto.class.getDeclaredField(nomeCampo);
        campo.setAccessible(true);
        return campo.get(null);
    }

    private void escreverCampoContexto(final String nomeCampo, final Object valor) throws Exception {
        final Field campo = AplicacaoContexto.class.getDeclaredField(nomeCampo);
        campo.setAccessible(true);
        campo.set(null, valor);
    }
}
