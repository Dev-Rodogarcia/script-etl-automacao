package br.com.extrator.aplicacao.validacao;

import static br.com.extrator.aplicacao.validacao.ValidacaoApiBanco24hDetalhadaTypes.EntidadeValidacao;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.extrator.dominio.graphql.usuarios.IndividualNodeDTO;
import br.com.extrator.integracao.ClienteApiDataExport;
import br.com.extrator.integracao.ClienteApiGraphQL;
import br.com.extrator.integracao.ResultadoExtracao;
import br.com.extrator.integracao.mapeamento.dataexport.contasapagar.ContasAPagarMapper;
import br.com.extrator.integracao.mapeamento.dataexport.cotacao.CotacaoMapper;
import br.com.extrator.integracao.mapeamento.dataexport.faturaporcliente.FaturaPorClienteMapper;
import br.com.extrator.integracao.mapeamento.dataexport.localizacaocarga.LocalizacaoCargaMapper;
import br.com.extrator.integracao.mapeamento.dataexport.manifestos.ManifestoMapper;
import br.com.extrator.integracao.mapeamento.graphql.coletas.ColetaMapper;
import br.com.extrator.integracao.mapeamento.graphql.fretes.FreteMapper;
import br.com.extrator.integracao.mapeamento.graphql.usuarios.UsuarioSistemaMapper;
import br.com.extrator.suporte.console.LoggerConsole;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

class ValidacaoApiBanco24hDetalhadaApiCollectorTest {

    @Test
    void deveUsarConsultaIncrementalDeUsuariosMesmoQuandoDimUsuariosEstiverVazia() throws Exception {
        final RecordingClienteApiGraphQL clienteGraphQL = new RecordingClienteApiGraphQL();
        final ValidacaoApiBanco24hDetalhadaApiCollector collector = new ValidacaoApiBanco24hDetalhadaApiCollector(
            new ClienteApiDataExport(),
            clienteGraphQL,
            new ManifestoMapper(),
            new CotacaoMapper(),
            new LocalizacaoCargaMapper(),
            new ContasAPagarMapper(),
            new FaturaPorClienteMapper(),
            new FreteMapper(),
            new ColetaMapper(),
            new UsuarioSistemaMapper(),
            new ValidacaoApiBanco24hDetalhadaMetadataHasher(),
            new ValidacaoApiBanco24hDetalhadaRepository(
                LoggerConsole.getLogger(ValidacaoApiBanco24hDetalhadaApiCollectorTest.class),
                new ValidacaoApiBanco24hDetalhadaMetadataHasher()
            )
        );
        final Connection conexao = conexaoQueFalhaSeConsultarDimUsuarios();
        final LocalDate dataInicio = LocalDate.of(2026, 3, 26);
        final LocalDate dataFim = LocalDate.of(2026, 3, 27);

        final List<EntidadeValidacao> entidades = collector.criarEntidades(
            conexao,
            dataFim,
            dataInicio,
            dataFim,
            List.of(ConstantesEntidades.USUARIOS_SISTEMA),
            false
        );
        final var resultado = entidades.get(0).fornecedor().get();

        assertEquals(1, entidades.size());
        assertTrue(clienteGraphQL.incrementalChamado);
        assertFalse(clienteGraphQL.fullLoadChamado);
        assertEquals(dataInicio, clienteGraphQL.dataInicioRecebida);
        assertEquals(dataFim, clienteGraphQL.dataFimRecebida);
        assertEquals(1, resultado.apiBruto());
        assertEquals(1, resultado.apiUnico());
        assertTrue(resultado.extracaoCompleta());
    }

    private Connection conexaoQueFalhaSeConsultarDimUsuarios() {
        return (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            (proxy, method, args) -> {
                if ("prepareStatement".equals(method.getName())) {
                    throw new AssertionError("A validacao de usuarios nao deve consultar dim_usuarios para decidir full load.");
                }
                if ("close".equals(method.getName())) {
                    return null;
                }
                if ("isClosed".equals(method.getName())) {
                    return false;
                }
                return valorPadrao(method.getReturnType());
            }
        );
    }

    private Object valorPadrao(final Class<?> tipoRetorno) {
        if (tipoRetorno == boolean.class) {
            return false;
        }
        if (tipoRetorno == int.class) {
            return 0;
        }
        if (tipoRetorno == long.class) {
            return 0L;
        }
        if (tipoRetorno == double.class) {
            return 0d;
        }
        return null;
    }

    private static final class RecordingClienteApiGraphQL extends ClienteApiGraphQL {
        private boolean incrementalChamado;
        private boolean fullLoadChamado;
        private LocalDate dataInicioRecebida;
        private LocalDate dataFimRecebida;

        @Override
        public ResultadoExtracao<IndividualNodeDTO> buscarUsuariosSistema(final LocalDate dataInicio, final LocalDate dataFim) {
            incrementalChamado = true;
            dataInicioRecebida = dataInicio;
            dataFimRecebida = dataFim;
            return ResultadoExtracao.completo(List.of(usuarioDto(10L, "Ana")), 1, 1);
        }

        @Override
        public ResultadoExtracao<IndividualNodeDTO> buscarUsuariosSistema() {
            fullLoadChamado = true;
            return ResultadoExtracao.completo(List.of(), 0, 0);
        }

        private static IndividualNodeDTO usuarioDto(final Long id, final String nome) {
            final IndividualNodeDTO dto = new IndividualNodeDTO();
            dto.setId(id);
            dto.setName(nome);
            return dto;
        }
    }
}
