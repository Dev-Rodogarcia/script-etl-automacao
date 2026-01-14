package br.com.extrator.runners.dataexport.extractors;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import br.com.extrator.api.ClienteApiDataExport;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.ContasAPagarDataExportEntity;
import br.com.extrator.db.repository.ContasAPagarRepository;
import br.com.extrator.modelo.dataexport.contasapagar.ContasAPagarDTO;
import br.com.extrator.modelo.dataexport.contasapagar.ContasAPagarMapper;
import br.com.extrator.runners.common.ConstantesExtracao;
import br.com.extrator.runners.common.DataExportEntityExtractor;
import br.com.extrator.runners.dataexport.services.Deduplicator;
import br.com.extrator.util.console.LoggerConsole;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Extractor para entidade Contas a Pagar (DataExport).
 * Inclui deduplicação antes de salvar.
 */
public class ContasAPagarExtractor implements DataExportEntityExtractor<ContasAPagarDTO> {
    
    private final ClienteApiDataExport apiClient;
    private final ContasAPagarRepository repository;
    private final ContasAPagarMapper mapper;
    private final LoggerConsole log;
    
    public ContasAPagarExtractor(final ClienteApiDataExport apiClient,
                                 final ContasAPagarRepository repository,
                                 final ContasAPagarMapper mapper,
                                 final LoggerConsole log) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.mapper = mapper;
        this.log = log;
    }
    
    @Override
    public ResultadoExtracao<ContasAPagarDTO> extract(final LocalDate dataInicio, final LocalDate dataFim) {
        // Contas a Pagar não usa filtro de data, busca últimas 24h
        return apiClient.buscarContasAPagar();
    }
    
    @Override
    public SaveResult saveWithDeduplication(final List<ContasAPagarDTO> dtos) throws java.sql.SQLException {
        if (dtos == null || dtos.isEmpty()) {
            return new SaveResult(0, 0);
        }
        
        final List<ContasAPagarDataExportEntity> entities = dtos.stream()
            .map(mapper::toEntity)
            .collect(Collectors.toList());
        
        // Deduplicar antes de salvar
        final List<ContasAPagarDataExportEntity> entitiesUnicos = Deduplicator.deduplicarFaturasAPagar(entities);
        final int totalUnicos = entitiesUnicos.size();
        
        if (entities.size() != entitiesUnicos.size()) {
            final int duplicadosRemovidos = entities.size() - entitiesUnicos.size();
            log.warn(ConstantesExtracao.MSG_LOG_DUPLICADOS_REMOVIDOS, duplicadosRemovidos);
        }
        
        final int registrosSalvos = repository.salvar(entitiesUnicos);
        return new SaveResult(registrosSalvos, totalUnicos);
    }
    
    @Override
    public int save(final List<ContasAPagarDTO> dtos) throws java.sql.SQLException {
        return saveWithDeduplication(dtos).getRegistrosSalvos();
    }
    
    @Override
    public String getEntityName() {
        return ConstantesEntidades.CONTAS_A_PAGAR;
    }
    
    @Override
    public String getEmoji() {
        return ConstantesExtracao.EMOJI_CONTAS_PAGAR;
    }
}
