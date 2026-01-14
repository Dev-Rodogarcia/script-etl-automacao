package br.com.extrator.runners.dataexport.extractors;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import br.com.extrator.api.ClienteApiDataExport;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.FaturaPorClienteEntity;
import br.com.extrator.db.repository.FaturaPorClienteRepository;
import br.com.extrator.modelo.dataexport.faturaporcliente.FaturaPorClienteDTO;
import br.com.extrator.modelo.dataexport.faturaporcliente.FaturaPorClienteMapper;
import br.com.extrator.runners.common.ConstantesExtracao;
import br.com.extrator.runners.common.DataExportEntityExtractor;
import br.com.extrator.runners.dataexport.services.Deduplicator;
import br.com.extrator.util.console.LoggerConsole;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Extractor para entidade Faturas por Cliente (DataExport).
 * Inclui deduplicação antes de salvar.
 */
public class FaturaPorClienteExtractor implements DataExportEntityExtractor<FaturaPorClienteDTO> {
    
    private final ClienteApiDataExport apiClient;
    private final FaturaPorClienteRepository repository;
    private final FaturaPorClienteMapper mapper;
    private final LoggerConsole log;
    
    public FaturaPorClienteExtractor(final ClienteApiDataExport apiClient,
                                    final FaturaPorClienteRepository repository,
                                    final FaturaPorClienteMapper mapper,
                                    final LoggerConsole log) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.mapper = mapper;
        this.log = log;
    }
    
    @Override
    public ResultadoExtracao<FaturaPorClienteDTO> extract(final LocalDate dataInicio, final LocalDate dataFim) {
        // Faturas por Cliente não usa filtro de data, busca últimas 24h
        return apiClient.buscarFaturasPorCliente();
    }
    
    @Override
    public SaveResult saveWithDeduplication(final List<FaturaPorClienteDTO> dtos) throws java.sql.SQLException {
        if (dtos == null || dtos.isEmpty()) {
            return new SaveResult(0, 0);
        }
        
        final List<FaturaPorClienteEntity> entities = dtos.stream()
            .map(mapper::toEntity)
            .collect(Collectors.toList());
        
        // Deduplicar antes de salvar
        final List<FaturaPorClienteEntity> entitiesUnicos = Deduplicator.deduplicarFaturasPorCliente(entities);
        final int totalUnicos = entitiesUnicos.size();
        
        if (entities.size() != entitiesUnicos.size()) {
            final int duplicadosRemovidos = entities.size() - entitiesUnicos.size();
            log.warn(ConstantesExtracao.MSG_LOG_DUPLICADOS_REMOVIDOS, duplicadosRemovidos);
        }
        
        final int registrosSalvos = repository.salvar(entitiesUnicos);
        return new SaveResult(registrosSalvos, totalUnicos);
    }
    
    @Override
    public int save(final List<FaturaPorClienteDTO> dtos) throws java.sql.SQLException {
        return saveWithDeduplication(dtos).getRegistrosSalvos();
    }
    
    @Override
    public String getEntityName() {
        return ConstantesEntidades.FATURAS_POR_CLIENTE;
    }
    
    @Override
    public String getEmoji() {
        return ConstantesExtracao.EMOJI_FATURAS_CLIENTE;
    }
}
