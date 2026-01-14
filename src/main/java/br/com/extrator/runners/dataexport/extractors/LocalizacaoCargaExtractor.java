package br.com.extrator.runners.dataexport.extractors;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import br.com.extrator.api.ClienteApiDataExport;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.LocalizacaoCargaEntity;
import br.com.extrator.db.repository.LocalizacaoCargaRepository;
import br.com.extrator.modelo.dataexport.localizacaocarga.LocalizacaoCargaDTO;
import br.com.extrator.modelo.dataexport.localizacaocarga.LocalizacaoCargaMapper;
import br.com.extrator.runners.common.ConstantesExtracao;
import br.com.extrator.runners.common.DataExportEntityExtractor;
import br.com.extrator.runners.dataexport.services.Deduplicator;
import br.com.extrator.util.console.LoggerConsole;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Extractor para entidade Localização de Cargas (DataExport).
 * Inclui deduplicação antes de salvar.
 */
public class LocalizacaoCargaExtractor implements DataExportEntityExtractor<LocalizacaoCargaDTO> {
    
    private final ClienteApiDataExport apiClient;
    private final LocalizacaoCargaRepository repository;
    private final LocalizacaoCargaMapper mapper;
    private final LoggerConsole log;
    
    public LocalizacaoCargaExtractor(final ClienteApiDataExport apiClient,
                                    final LocalizacaoCargaRepository repository,
                                    final LocalizacaoCargaMapper mapper,
                                    final LoggerConsole log) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.mapper = mapper;
        this.log = log;
    }
    
    @Override
    public ResultadoExtracao<LocalizacaoCargaDTO> extract(final LocalDate dataInicio, final LocalDate dataFim) {
        // Localização de Cargas não usa filtro de data, busca últimas 24h
        return apiClient.buscarLocalizacaoCarga();
    }
    
    @Override
    public SaveResult saveWithDeduplication(final List<LocalizacaoCargaDTO> dtos) throws java.sql.SQLException {
        if (dtos == null || dtos.isEmpty()) {
            return new SaveResult(0, 0);
        }
        
        final List<LocalizacaoCargaEntity> entities = dtos.stream()
            .map(mapper::toEntity)
            .collect(Collectors.toList());
        
        // Deduplicar antes de salvar
        final List<LocalizacaoCargaEntity> entitiesUnicos = Deduplicator.deduplicarLocalizacoes(entities);
        final int totalUnicos = entitiesUnicos.size();
        
        if (entities.size() != entitiesUnicos.size()) {
            final int duplicadosRemovidos = entities.size() - entitiesUnicos.size();
            log.warn(ConstantesExtracao.MSG_LOG_DUPLICADOS_REMOVIDOS, duplicadosRemovidos);
        }
        
        final int registrosSalvos = repository.salvar(entitiesUnicos);
        return new SaveResult(registrosSalvos, totalUnicos);
    }
    
    @Override
    public int save(final List<LocalizacaoCargaDTO> dtos) throws java.sql.SQLException {
        return saveWithDeduplication(dtos).getRegistrosSalvos();
    }
    
    @Override
    public String getEntityName() {
        return ConstantesEntidades.LOCALIZACAO_CARGAS;
    }
    
    @Override
    public String getEmoji() {
        return ConstantesExtracao.EMOJI_LOCALIZACAO;
    }
}
