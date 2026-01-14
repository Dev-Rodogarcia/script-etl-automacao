package br.com.extrator.runners.dataexport;

import java.time.LocalDate;

import br.com.extrator.runners.dataexport.services.DataExportExtractionService;
import br.com.extrator.util.console.LoggerConsole;

/**
 * Runner independente para a API Data Export (Manifestos, Cotações e Localização de Carga).
 * Refatorado para usar serviços de orquestração.
 */
public final class DataExportRunner {
    private static final LoggerConsole log = LoggerConsole.getLogger(DataExportRunner.class);

    private DataExportRunner() {}

    /**
     * Executa extração de todas as entidades Data Export.
     * 
     * @param dataInicio Data de início para filtro
     * @throws Exception Se houver falha na extração
     */
    public static void executar(final LocalDate dataInicio) throws Exception {
        executar(dataInicio, null);
    }

    /**
     * Executa extração de todas as entidades Data Export para um intervalo de datas.
     * 
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @throws Exception Se houver falha na extração
     */
    public static void executarPorIntervalo(final LocalDate dataInicio, final LocalDate dataFim) throws Exception {
        executarPorIntervalo(dataInicio, dataFim, null);
    }

    /**
     * Executa extração de entidade(s) Data Export específica(s) para um intervalo de datas.
     * 
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @param entidade Nome da entidade (null = todas)
     * @throws Exception Se houver falha na extração
     */
    public static void executarPorIntervalo(final LocalDate dataInicio, final LocalDate dataFim, final String entidade) throws Exception {
        log.info("🔄 Executando runner DataExport - Período: {} a {}", dataInicio, dataFim);
        
        final DataExportExtractionService service = new DataExportExtractionService();
        service.execute(dataInicio, dataFim, entidade);
    }

    /**
     * Executa extração de entidade(s) Data Export específica(s).
     * 
     * @param dataInicio Data de início para filtro
     * @param entidade Nome da entidade (null = todas)
     * @throws Exception Se houver falha na extração
     */
    public static void executar(final LocalDate dataInicio, final String entidade) throws Exception {
        log.info("🔄 Executando runner DataExport...");
        
        final DataExportExtractionService service = new DataExportExtractionService();
        service.execute(dataInicio, dataInicio, entidade);
    }
}
