package br.com.extrator.runners.dataexport.extractors;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import br.com.extrator.api.ClienteApiDataExport;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.CotacaoEntity;
import br.com.extrator.db.repository.CotacaoRepository;
import br.com.extrator.modelo.dataexport.cotacao.CotacaoDTO;
import br.com.extrator.modelo.dataexport.cotacao.CotacaoMapper;
import br.com.extrator.runners.common.ConstantesExtracao;
import br.com.extrator.runners.common.DataExportEntityExtractor;
import br.com.extrator.runners.dataexport.services.Deduplicator;
import br.com.extrator.util.console.LoggerConsole;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Extractor para entidade Cotações (DataExport).
 * Inclui deduplicação antes de salvar.
 */
public class CotacaoExtractor implements DataExportEntityExtractor<CotacaoDTO> {
    
    private final ClienteApiDataExport apiClient;
    private final CotacaoRepository repository;
    private final CotacaoMapper mapper;
    private final LoggerConsole log;
    
    public CotacaoExtractor(final ClienteApiDataExport apiClient,
                           final CotacaoRepository repository,
                           final CotacaoMapper mapper,
                           final LoggerConsole log) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.mapper = mapper;
        this.log = log;
    }
    
    @Override
    public ResultadoExtracao<CotacaoDTO> extract(final LocalDate dataInicio, final LocalDate dataFim) {
        // Usa intervalo informado quando disponível; fallback para últimas 24h
        if (dataInicio != null) {
            final LocalDate fim = (dataFim != null) ? dataFim : dataInicio;
            return apiClient.buscarCotacoes(dataInicio, fim);
        }
        return apiClient.buscarCotacoes();
    }
    
    @Override
    public SaveResult saveWithDeduplication(final List<CotacaoDTO> dtos) throws java.sql.SQLException {
        if (dtos == null || dtos.isEmpty()) {
            return new SaveResult(0, 0);
        }
        
        final List<CotacaoEntity> entities = dtos.stream()
            .map(mapper::toEntity)
            .collect(Collectors.toList());
        
        // Deduplicar antes de salvar
        final List<CotacaoEntity> entitiesUnicos = Deduplicator.deduplicarCotacoes(entities);
        final int totalUnicos = entitiesUnicos.size();
        
        if (entities.size() != entitiesUnicos.size()) {
            final int duplicadosRemovidos = entities.size() - entitiesUnicos.size();
            log.warn(ConstantesExtracao.MSG_LOG_DUPLICADOS_REMOVIDOS, duplicadosRemovidos);
        }
        
        final int registrosSalvos = repository.salvar(entitiesUnicos);
        return new SaveResult(registrosSalvos, totalUnicos);
    }
    
    @Override
    public int save(final List<CotacaoDTO> dtos) throws java.sql.SQLException {
        return saveWithDeduplication(dtos).getRegistrosSalvos();
    }
    
    @Override
    public String getEntityName() {
        return ConstantesEntidades.COTACOES;
    }
    
    @Override
    public String getEmoji() {
        return ConstantesExtracao.EMOJI_COTACOES;
    }
}
