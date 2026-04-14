package br.com.extrator.integracao.graphql.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.contexto.AplicacaoContexto;
import br.com.extrator.aplicacao.portas.ExecutionAuditPort;
import br.com.extrator.integracao.comum.ExtractionLogger;
import br.com.extrator.integracao.comum.ExtractionResult;
import br.com.extrator.persistencia.entidade.LogExtracaoEntity;
import br.com.extrator.persistencia.repositorio.LogExtracaoRepository;
import br.com.extrator.plataforma.auditoria.dominio.ExecutionAuditRecord;
import br.com.extrator.suporte.console.LoggerConsole;
import br.com.extrator.suporte.observabilidade.ExecutionContext;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

class GraphQLExtractionServiceTest {

    @AfterEach
    void limparContexto() {
        ExecutionContext.clear();
    }

    @Test
    void deveBloquearFretesQuandoColetasNaoConcluirComSucessoNoMesmoCiclo() {
        final TestableGraphQLExtractionService service = new TestableGraphQLExtractionService(
            resultado(ConstantesEntidades.USUARIOS_SISTEMA, ConstantesEntidades.STATUS_COMPLETO, true, "usuarios ok"),
            resultado(ConstantesEntidades.COLETAS, ConstantesEntidades.STATUS_INCOMPLETO_DADOS, false, "coletas incompleto"),
            resultado(ConstantesEntidades.FRETES, ConstantesEntidades.STATUS_COMPLETO, true, "fretes ok")
        );

        final RuntimeException erro = assertThrows(
            RuntimeException.class,
            () -> service.execute(LocalDate.of(2026, 3, 18), LocalDate.of(2026, 3, 18), null)
        );

        assertFalse(service.fretesExecutado, "Fretes nao deve iniciar quando Coletas falha no mesmo ciclo");
        assertTrue(
            service.logsGerados.stream().anyMatch(log -> log.getEntidade().equals(ConstantesEntidades.FRETES)
                && ConstantesEntidades.STATUS_INCOMPLETO_DADOS.equals(log.getStatusFinal().getValor())),
            "Bloqueio de Fretes deve ser gravado em log_extracoes com status explicito"
        );
        assertTrue(erro.getMessage().contains("Fretes") || erro.getMessage().contains("fretes"));
        assertEquals(3, service.logsGerados.size(), "Usuarios, Coletas e o bloqueio de Fretes devem ser auditados");
    }

    @Test
    void naoDeveRegistrarAuditoriaEstruturadaQuandoExecucaoAuxiliarDesativarAuditoria() {
        final RecordingExecutionAuditPort auditPort = new RecordingExecutionAuditPort();
        final RecordingLogExtracaoRepository logRepository = new RecordingLogExtracaoRepository();
        final AuditToggleGraphQLExtractionService service =
            new AuditToggleGraphQLExtractionService(logRepository, auditPort, false);

        service.registrar(resultado(ConstantesEntidades.COLETAS, ConstantesEntidades.STATUS_COMPLETO, true, "coletas ok"));

        assertEquals(1, logRepository.logs.size(), "Log operacional da entidade deve continuar sendo gravado.");
        assertEquals(0, auditPort.records.size(), "Execucao auxiliar nao deve contaminar sys_execution_audit.");
    }

    @Test
    void deveRegistrarAuditoriaEstruturadaQuandoModoPadraoEstiverAtivo() {
        final RecordingExecutionAuditPort auditPort = new RecordingExecutionAuditPort();
        final RecordingLogExtracaoRepository logRepository = new RecordingLogExtracaoRepository();
        final AuditToggleGraphQLExtractionService service =
            new AuditToggleGraphQLExtractionService(logRepository, auditPort, true);

        try {
            ExecutionContext.initialize("--teste-graph-ql");
            service.registrar(resultado(ConstantesEntidades.COLETAS, ConstantesEntidades.STATUS_COMPLETO, true, "coletas ok"));
        } finally {
            ExecutionContext.clear();
        }

        assertEquals(1, logRepository.logs.size());
        assertEquals(1, auditPort.records.size(), "Execucao principal deve continuar auditando normalmente.");
        assertEquals(ConstantesEntidades.COLETAS, auditPort.records.get(0).entidade());
    }

    @Test
    void execucaoAuxiliarDeColetasNaoDeveGravarLogOperacionalNoBanco() {
        final AuxiliaryColetasGraphQLExtractionService service = new AuxiliaryColetasGraphQLExtractionService();

        service.executarSomenteColetasReferencial(LocalDate.of(2026, 4, 7), LocalDate.of(2026, 4, 8));

        assertTrue(service.coletasReferencialExecutado, "Execucao auxiliar deve usar o fluxo dedicado.");
        assertFalse(service.coletasPrincipalExecutado, "Execucao auxiliar nao deve registrar Coletas no namespace principal.");
        assertEquals(0, service.logRepository.logs.size(), "Namespace auxiliar nao deve persistir em log_extracoes.");
    }

    @Test
    void modoOperacionalNaoDeveAceitarEntidadeCoreIncompletaComoSucesso() {
        final String valorAnterior = System.getProperty("etl.integridade.modo");
        System.setProperty("etl.integridade.modo", "OPERACIONAL");
        try {
            final TestableGraphQLExtractionService service = new TestableGraphQLExtractionService(
                resultado(ConstantesEntidades.USUARIOS_SISTEMA, ConstantesEntidades.STATUS_COMPLETO, true, "usuarios ok"),
                resultado(ConstantesEntidades.COLETAS, ConstantesEntidades.STATUS_INCOMPLETO_DADOS, false, "coletas incompleto"),
                resultado(ConstantesEntidades.FRETES, ConstantesEntidades.STATUS_COMPLETO, true, "fretes ok")
            );

            final RuntimeException erro = assertThrows(
                RuntimeException.class,
                () -> service.execute(LocalDate.of(2026, 3, 18), LocalDate.of(2026, 3, 18), null)
            );

            assertNotNull(erro.getMessage());
            assertTrue(erro.getMessage().contains(ConstantesEntidades.COLETAS));
        } finally {
            restaurarPropriedade("etl.integridade.modo", valorAnterior);
        }
    }

    @Test
    void naoDevePersistirResultadosIntermediariosQuandoTentativaFalhaESeraRetentada() {
        final RecordingExecutionAuditPort auditPort = new RecordingExecutionAuditPort();
        final RecordingLogExtracaoRepository logRepository = new RecordingLogExtracaoRepository();
        final BufferedGraphQLExtractionService service = new BufferedGraphQLExtractionService(
            resultado(ConstantesEntidades.USUARIOS_SISTEMA, ConstantesEntidades.STATUS_COMPLETO, true, "usuarios ok"),
            resultado(ConstantesEntidades.COLETAS, ConstantesEntidades.STATUS_COMPLETO, true, "coletas ok"),
            resultado(ConstantesEntidades.FRETES, ConstantesEntidades.STATUS_ERRO_API, false, "fretes falhou"),
            logRepository,
            auditPort
        );

        ExecutionContext.initialize("--loop-daemon-run");
        ExecutionContext.setRetryContext(1, 3);

        assertThrows(
            RuntimeException.class,
            () -> service.execute(LocalDate.of(2026, 3, 18), LocalDate.of(2026, 3, 18), null)
        );

        assertEquals(0, logRepository.logs.size(), "Tentativa intermediaria nao deve gerar log_extracoes oficial.");
        assertEquals(0, auditPort.records.size(), "Tentativa intermediaria nao deve contaminar a auditoria estruturada.");
    }

    @Test
    void devePersistirResultadosNaTentativaFinalMesmoQuandoFalhar() {
        final RecordingExecutionAuditPort auditPort = new RecordingExecutionAuditPort();
        final RecordingLogExtracaoRepository logRepository = new RecordingLogExtracaoRepository();
        final BufferedGraphQLExtractionService service = new BufferedGraphQLExtractionService(
            resultado(ConstantesEntidades.USUARIOS_SISTEMA, ConstantesEntidades.STATUS_COMPLETO, true, "usuarios ok"),
            resultado(ConstantesEntidades.COLETAS, ConstantesEntidades.STATUS_COMPLETO, true, "coletas ok"),
            resultado(ConstantesEntidades.FRETES, ConstantesEntidades.STATUS_ERRO_API, false, "fretes falhou"),
            logRepository,
            auditPort
        );

        ExecutionContext.initialize("--loop-daemon-run");
        ExecutionContext.setRetryContext(3, 3);

        assertThrows(
            RuntimeException.class,
            () -> service.execute(LocalDate.of(2026, 3, 18), LocalDate.of(2026, 3, 18), null)
        );

        assertEquals(3, logRepository.logs.size(), "Tentativa final deve persistir o conjunto completo de resultados.");
        assertEquals(3, auditPort.records.size(), "Tentativa final deve registrar auditoria para cada entidade executada.");
        assertTrue(
            logRepository.logs.stream().anyMatch(log -> ConstantesEntidades.FRETES.equals(log.getEntidade())
                && ConstantesEntidades.STATUS_ERRO_API.equals(log.getStatusFinal().getValor())),
            "A falha final de Fretes precisa permanecer visivel para diagnostico."
        );
    }

    private static ExtractionResult resultado(final String entidade,
                                              final String status,
                                              final boolean sucesso,
                                              final String mensagem) {
        return new ExtractionResult.Builder(entidade, LocalDateTime.of(2026, 3, 18, 10, 0))
            .fim(LocalDateTime.of(2026, 3, 18, 10, 1))
            .status(status)
            .registrosSalvos(sucesso ? 10 : 0)
            .registrosExtraidos(sucesso ? 10 : 0)
            .totalUnicos(sucesso ? 10 : 0)
            .paginasProcessadas(sucesso ? 2 : 0)
            .mensagem(mensagem)
            .sucesso(sucesso)
            .build();
    }

    private static final class TestableGraphQLExtractionService extends GraphQLExtractionService {
        private final ExtractionResult usuariosResult;
        private final ExtractionResult coletasResult;
        private final ExtractionResult fretesResult;
        private final List<br.com.extrator.persistencia.entidade.LogExtracaoEntity> logsGerados = new ArrayList<>();
        private boolean fretesExecutado;

        private TestableGraphQLExtractionService(final ExtractionResult usuariosResult,
                                                 final ExtractionResult coletasResult,
                                                 final ExtractionResult fretesResult) {
            super(null, new LogExtracaoRepository(), AplicacaoContexto.executionAuditPort(),
                new ExtractionLogger(TestableGraphQLExtractionService.class),
                LoggerConsole.getLogger(TestableGraphQLExtractionService.class));
            this.usuariosResult = usuariosResult;
            this.coletasResult = coletasResult;
            this.fretesResult = fretesResult;
        }

        @Override
        protected void validarInfraestrutura() {
            // no-op
        }

        @Override
        protected void aplicarDelayEntreEntidades() {
            // no-op
        }

        @Override
        protected void registrarLogExtracao(final ExtractionResult result) {
            logsGerados.add(result.toLogEntity());
        }

        @Override
        protected ExtractionResult extractUsuarios(final LocalDate dataInicio, final LocalDate dataFim, final boolean throwOnError) {
            registrarResultadoExecucao(usuariosResult);
            return usuariosResult;
        }

        @Override
        protected ExtractionResult extractColetas(final LocalDate dataInicio, final LocalDate dataFim) {
            registrarResultadoExecucao(coletasResult);
            return coletasResult;
        }

        @Override
        protected ExtractionResult extractFretes(final LocalDate dataInicio, final LocalDate dataFim) {
            fretesExecutado = true;
            registrarResultadoExecucao(fretesResult);
            return fretesResult;
        }
    }

    private static final class AuditToggleGraphQLExtractionService extends GraphQLExtractionService {
        private AuditToggleGraphQLExtractionService(final LogExtracaoRepository logRepository,
                                                    final ExecutionAuditPort executionAuditPort,
                                                    final boolean auditoriaEstruturadaAtiva) {
            super(
                null,
                logRepository,
                executionAuditPort,
                new ExtractionLogger(AuditToggleGraphQLExtractionService.class),
                LoggerConsole.getLogger(AuditToggleGraphQLExtractionService.class),
                auditoriaEstruturadaAtiva
            );
        }

        private void registrar(final ExtractionResult result) {
            registrarLogExtracao(result);
        }
    }

    private static final class AuxiliaryColetasGraphQLExtractionService extends GraphQLExtractionService {
        private final RecordingLogExtracaoRepository logRepository;
        private boolean coletasPrincipalExecutado;
        private boolean coletasReferencialExecutado;

        private AuxiliaryColetasGraphQLExtractionService() {
            this(new RecordingLogExtracaoRepository());
        }

        private AuxiliaryColetasGraphQLExtractionService(final RecordingLogExtracaoRepository logRepository) {
            super(
                null,
                logRepository,
                AplicacaoContexto.executionAuditPort(),
                new ExtractionLogger(AuxiliaryColetasGraphQLExtractionService.class),
                LoggerConsole.getLogger(AuxiliaryColetasGraphQLExtractionService.class),
                false
            );
            this.logRepository = logRepository;
        }

        @Override
        protected void validarInfraestrutura() {
            // no-op
        }

        @Override
        protected ExtractionResult extractColetas(final LocalDate dataInicio, final LocalDate dataFim) {
            coletasPrincipalExecutado = true;
            final ExtractionResult result = resultado(ConstantesEntidades.COLETAS, ConstantesEntidades.STATUS_COMPLETO, true, "coletas principal");
            registrarResultadoExecucao(result);
            return result;
        }

        @Override
        protected ExtractionResult extractColetasReferencial(final LocalDate dataInicio, final LocalDate dataFim) {
            coletasReferencialExecutado = true;
            final ExtractionResult result =
                resultado(ConstantesEntidades.COLETAS_REFERENCIAL, ConstantesEntidades.STATUS_COMPLETO, true, "coletas auxiliar");
            registrarResultadoExecucao(result);
            return result;
        }
    }

    private static final class BufferedGraphQLExtractionService extends GraphQLExtractionService {
        private final ExtractionResult usuariosResult;
        private final ExtractionResult coletasResult;
        private final ExtractionResult fretesResult;

        private BufferedGraphQLExtractionService(final ExtractionResult usuariosResult,
                                                 final ExtractionResult coletasResult,
                                                 final ExtractionResult fretesResult,
                                                 final LogExtracaoRepository logRepository,
                                                 final ExecutionAuditPort executionAuditPort) {
            super(
                null,
                logRepository,
                executionAuditPort,
                new ExtractionLogger(BufferedGraphQLExtractionService.class),
                LoggerConsole.getLogger(BufferedGraphQLExtractionService.class),
                true
            );
            this.usuariosResult = usuariosResult;
            this.coletasResult = coletasResult;
            this.fretesResult = fretesResult;
        }

        @Override
        protected void validarInfraestrutura() {
            // no-op
        }

        @Override
        protected void aplicarDelayEntreEntidades() {
            // no-op
        }

        @Override
        protected ExtractionResult extractUsuarios(final LocalDate dataInicio, final LocalDate dataFim, final boolean throwOnError) {
            registrarResultadoExecucao(usuariosResult);
            return usuariosResult;
        }

        @Override
        protected ExtractionResult extractColetas(final LocalDate dataInicio, final LocalDate dataFim) {
            registrarResultadoExecucao(coletasResult);
            return coletasResult;
        }

        @Override
        protected ExtractionResult extractFretes(final LocalDate dataInicio, final LocalDate dataFim) {
            registrarResultadoExecucao(fretesResult);
            return fretesResult;
        }
    }

    private static final class RecordingLogExtracaoRepository extends LogExtracaoRepository {
        private final List<LogExtracaoEntity> logs = new ArrayList<>();

        @Override
        public void gravarLogExtracao(final LogExtracaoEntity logExtracao) {
            logs.add(logExtracao);
        }
    }

    private static final class RecordingExecutionAuditPort implements ExecutionAuditPort {
        private final List<ExecutionAuditRecord> records = new ArrayList<>();

        @Override
        public void registrarResultado(final ExecutionAuditRecord record) {
            records.add(record);
        }

        @Override
        public Optional<ExecutionAuditRecord> buscarResultado(final String executionUuid, final String entidade) {
            return Optional.empty();
        }

        @Override
        public List<ExecutionAuditRecord> listarResultados(final String executionUuid) {
            return List.of();
        }

        @Override
        public Optional<LocalDateTime> buscarWatermarkConfirmado(final String entidade) {
            return Optional.empty();
        }

        @Override
        public void atualizarWatermarkConfirmado(final String entidade, final LocalDateTime watermarkConfirmado) {
            // no-op
        }
    }

    private void restaurarPropriedade(final String chave, final String valorAnterior) {
        if (valorAnterior == null) {
            System.clearProperty(chave);
            return;
        }
        System.setProperty(chave, valorAnterior);
    }
}
