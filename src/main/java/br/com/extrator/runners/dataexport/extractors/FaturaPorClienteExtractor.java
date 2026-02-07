package br.com.extrator.runners.dataexport.extractors;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        // Usa intervalo informado quando disponível; fallback para últimas 24h
        if (dataInicio != null) {
            final LocalDate fim = (dataFim != null) ? dataFim : dataInicio;
            return apiClient.buscarFaturasPorCliente(dataInicio, fim);
        }
        return apiClient.buscarFaturasPorCliente();
    }
    
    @Override
    public SaveResult saveWithDeduplication(final List<FaturaPorClienteDTO> dtos) throws java.sql.SQLException {
        if (dtos == null || dtos.isEmpty()) {
            return new SaveResult(0, 0);
        }
        
        // PASSO 1: Deduplicar no nível DTO (antes de converter para Entity)
        // Usa o unique_id calculado como chave única
        final Map<String, FaturaPorClienteDTO> faturasUnicas = new HashMap<>();
        for (final FaturaPorClienteDTO dto : dtos) {
            // Calcular unique_id temporariamente para deduplicação
            final String uniqueId = calcularUniqueIdTemporario(dto);
            if (uniqueId != null && !faturasUnicas.containsKey(uniqueId)) {
                faturasUnicas.put(uniqueId, dto);
            }
        }
        
        final int duplicadosRemovidos = dtos.size() - faturasUnicas.size();
        if (duplicadosRemovidos > 0) {
            log.warn("⚠️ {} faturas duplicadas removidas no nível DTO (antes do enriquecimento GraphQL)", 
                duplicadosRemovidos);
        }
        
        // PASSO 2: Converter DTOs únicos para Entities
        final List<FaturaPorClienteEntity> entities = faturasUnicas.values().stream()
            .map(mapper::toEntity)
            .collect(Collectors.toList());
        
        // PASSO 3: Deduplicar novamente no nível Entity (proteção adicional)
        final List<FaturaPorClienteEntity> entitiesUnicos = Deduplicator.deduplicarFaturasPorCliente(entities);
        final int totalUnicos = entitiesUnicos.size();
        
        if (entities.size() != entitiesUnicos.size()) {
            final int duplicadosEntity = entities.size() - entitiesUnicos.size();
            log.warn("⚠️ {} duplicados adicionais removidos no nível Entity", duplicadosEntity);
        }
        
        // PASSO 4: Salvar no banco
        final int registrosSalvos = repository.salvar(entitiesUnicos);
        return new SaveResult(registrosSalvos, totalUnicos);
    }
    
    /**
     * Calcula o unique_id temporariamente para deduplicação no nível DTO.
     * Usa a mesma lógica do Mapper: CT-e key > NFS-e number > UUID.
     */
    private String calcularUniqueIdTemporario(final FaturaPorClienteDTO dto) {
        if (dto.getNfseNumber() != null) {
            return "NFSE-" + dto.getNfseNumber();
        }
        if (dto.getCteKey() != null && !dto.getCteKey().trim().isEmpty()) {
            return dto.getCteKey().trim();
        }
        // Fallback: usar fatura document se disponível
        if (dto.getFaturaDocument() != null && !dto.getFaturaDocument().trim().isEmpty()) {
            return "FATURA-" + dto.getFaturaDocument().trim();
        }
        return null;
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
