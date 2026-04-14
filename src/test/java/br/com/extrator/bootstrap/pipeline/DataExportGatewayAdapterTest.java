package br.com.extrator.bootstrap.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import br.com.extrator.aplicacao.pipeline.runtime.StepExecutionResult;
import br.com.extrator.integracao.dataexport.services.DataExportExtractionService;
import br.com.extrator.suporte.configuracao.ConfigEtl;
import br.com.extrator.suporte.observabilidade.ExecutionContext;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

class DataExportGatewayAdapterTest {

    @AfterEach
    void limparContexto() {
        ExecutionContext.clear();
        System.clearProperty("ETL_PROCESS_ISOLATION_ENABLED");
        System.clearProperty("etl.process.isolation.enabled");
    }

    @Test
    void deveForcarIsolamentoNoLoopDaemonMesmoComFlagGlobalDesativada() throws Exception {
        System.setProperty("ETL_PROCESS_ISOLATION_ENABLED", "false");
        ExecutionContext.initialize("--loop-daemon-run");
        ExecutionContext.setCycleId("cycle-daemon");

        final RecordingDataExportService service = new RecordingDataExportService();
        final RecordingIsolatedExecutor isolatedExecutor = new RecordingIsolatedExecutor();
        final DataExportGatewayAdapter adapter = new DataExportGatewayAdapter(service, isolatedExecutor);

        final StepExecutionResult result = adapter.executar(
            LocalDate.of(2026, 3, 18),
            LocalDate.of(2026, 3, 18),
            "dataexport"
        );

        assertTrue(isolatedExecutor.executado);
        assertFalse(service.executado);
        assertEquals(IsolatedStepProcessExecutor.ApiType.DATAEXPORT, isolatedExecutor.apiType);
        assertEquals(ConfigEtl.obterTimeoutStepDataExport(), isolatedExecutor.timeout);
        assertEquals("isolated_process", result.getMetadata().get("execution_mode"));
        assertEquals(Boolean.TRUE, result.getMetadata().get("forced_by_daemon"));
    }

    @Test
    void deveUsarTimeoutDaEntidadeQuandoExecutaDataExportEspecifico() throws Exception {
        System.setProperty("ETL_PROCESS_ISOLATION_ENABLED", "true");

        final RecordingDataExportService service = new RecordingDataExportService();
        final RecordingIsolatedExecutor isolatedExecutor = new RecordingIsolatedExecutor();
        final DataExportGatewayAdapter adapter = new DataExportGatewayAdapter(service, isolatedExecutor);

        adapter.executar(
            LocalDate.of(2026, 3, 18),
            LocalDate.of(2026, 3, 18),
            ConstantesEntidades.MANIFESTOS
        );

        assertEquals(Duration.ofMinutes(15), isolatedExecutor.timeout);
    }

    private static final class RecordingDataExportService extends DataExportExtractionService {
        private boolean executado;

        @Override
        public void executar(final LocalDate dataInicio, final LocalDate dataFim, final String entidade) {
            executado = true;
        }
    }

    private static final class RecordingIsolatedExecutor extends IsolatedStepProcessExecutor {
        private boolean executado;
        private ApiType apiType;
        private Duration timeout;

        @Override
        public ProcessExecutionResult executar(final ApiType apiType,
                                               final LocalDate dataInicio,
                                               final LocalDate dataFim,
                                               final String entidade,
                                               final Duration timeout) {
            this.executado = true;
            this.apiType = apiType;
            this.timeout = timeout;
            return new ProcessExecutionResult(778L, Path.of("logs", "isolated_dataexport_test.log"));
        }
    }
}
