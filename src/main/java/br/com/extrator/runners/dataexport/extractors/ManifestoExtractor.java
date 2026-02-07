package br.com.extrator.runners.dataexport.extractors;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import br.com.extrator.api.ClienteApiDataExport;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.ManifestoEntity;
import br.com.extrator.db.repository.ManifestoRepository;
import br.com.extrator.modelo.dataexport.manifestos.ManifestoDTO;
import br.com.extrator.modelo.dataexport.manifestos.ManifestoMapper;
import br.com.extrator.runners.common.ConstantesExtracao;
import br.com.extrator.runners.common.DataExportEntityExtractor;
import br.com.extrator.runners.dataexport.services.Deduplicator;
import br.com.extrator.util.console.LoggerConsole;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Extractor para entidade Manifestos (DataExport).
 * Inclui deduplicação antes de salvar.
 */
public class ManifestoExtractor implements DataExportEntityExtractor<ManifestoDTO> {
    
    private final ClienteApiDataExport apiClient;
    private final ManifestoRepository repository;
    private final ManifestoMapper mapper;
    private final LoggerConsole log;
    
    public ManifestoExtractor(final ClienteApiDataExport apiClient,
                             final ManifestoRepository repository,
                             final ManifestoMapper mapper,
                             final LoggerConsole log) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.mapper = mapper;
        this.log = log;
    }
    
    @Override
    public ResultadoExtracao<ManifestoDTO> extract(final LocalDate dataInicio, final LocalDate dataFim) {
        // Usa intervalo informado quando disponível; fallback para últimas 24h
        if (dataInicio != null) {
            final LocalDate fim = (dataFim != null) ? dataFim : dataInicio;
            return apiClient.buscarManifestos(dataInicio, fim);
        }
        return apiClient.buscarManifestos();
    }
    
    @Override
    public SaveResult saveWithDeduplication(final List<ManifestoDTO> dtos) throws java.sql.SQLException {
        if (dtos == null || dtos.isEmpty()) {
            return new SaveResult(0, 0);
        }
        
        final List<ManifestoEntity> entities = dtos.stream()
            .map(mapper::toEntity)
            .collect(Collectors.toList());
        
        // Deduplicar antes de salvar
        final List<ManifestoEntity> entitiesUnicos = Deduplicator.deduplicarManifestos(entities);
        final int totalUnicos = entitiesUnicos.size();
        
        if (entities.size() != entitiesUnicos.size()) {
            final int duplicadosRemovidos = entities.size() - entitiesUnicos.size();
            log.warn(ConstantesExtracao.MSG_LOG_DUPLICADOS_REMOVIDOS, duplicadosRemovidos);
        }
        
        final int registrosSalvos = repository.salvar(entitiesUnicos);
        return new SaveResult(registrosSalvos, totalUnicos);
    }
    
    @Override
    public int save(final List<ManifestoDTO> dtos) throws java.sql.SQLException {
        return saveWithDeduplication(dtos).getRegistrosSalvos();
    }
    
    @Override
    public String getEntityName() {
        return ConstantesEntidades.MANIFESTOS;
    }
    
    @Override
    public String getEmoji() {
        return ConstantesExtracao.EMOJI_FATURAS;
    }
}
