package br.com.extrator.runners.dataexport.extractors;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.extrator.api.ClienteApiDataExport;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.LocalizacaoCargaEntity;
import br.com.extrator.db.repository.InvalidRecordAuditRepository;
import br.com.extrator.db.repository.LocalizacaoCargaRepository;
import br.com.extrator.modelo.dataexport.localizacaocarga.LocalizacaoCargaDTO;
import br.com.extrator.modelo.dataexport.localizacaocarga.LocalizacaoCargaMapper;
import br.com.extrator.runners.common.ConstantesExtracao;
import br.com.extrator.runners.common.DataExportEntityExtractor;
import br.com.extrator.runners.dataexport.services.Deduplicator;
import br.com.extrator.util.console.LoggerConsole;
import br.com.extrator.util.mapeamento.MapperUtil;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Extractor para entidade LocalizaÃ§Ã£o de Cargas (DataExport).
 * Inclui deduplicaÃ§Ã£o antes de salvar.
 */
public class LocalizacaoCargaExtractor implements DataExportEntityExtractor<LocalizacaoCargaDTO> {
    
    private final ClienteApiDataExport apiClient;
    private final LocalizacaoCargaRepository repository;
    private final LocalizacaoCargaMapper mapper;
    private final LoggerConsole log;
    private final InvalidRecordAuditRepository invalidRecordAuditRepository;
    
    public LocalizacaoCargaExtractor(final ClienteApiDataExport apiClient,
                                    final LocalizacaoCargaRepository repository,
                                    final LocalizacaoCargaMapper mapper,
                                    final LoggerConsole log) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.mapper = mapper;
        this.log = log;
        this.invalidRecordAuditRepository = new InvalidRecordAuditRepository();
    }
    
    @Override
    public ResultadoExtracao<LocalizacaoCargaDTO> extract(final LocalDate dataInicio, final LocalDate dataFim) {
        // Usa intervalo informado quando disponÃ­vel; fallback para Ãºltimas 24h
        if (dataInicio != null) {
            final LocalDate fim = (dataFim != null) ? dataFim : dataInicio;
            return apiClient.buscarLocalizacaoCarga(dataInicio, fim);
        }
        return apiClient.buscarLocalizacaoCarga();
    }
    
    @Override
    public SaveResult saveWithDeduplication(final List<LocalizacaoCargaDTO> dtos) throws java.sql.SQLException {
        if (dtos == null || dtos.isEmpty()) {
            return new SaveResult(0, 0);
        }
        
        final List<LocalizacaoCargaEntity> entities = new ArrayList<>();
        int registrosInvalidos = 0;
        for (final LocalizacaoCargaDTO dto : dtos) {
            try {
                final LocalizacaoCargaEntity entity = mapper.toEntity(dto);
                if (entity != null) {
                    entities.add(entity);
                } else {
                    registrosInvalidos++;
                    auditarRegistroInvalido(dto, "MAPPER_RETORNOU_NULL", "Mapper retornou entidade nula.");
                }
            } catch (final RuntimeException e) {
                registrosInvalidos++;
                auditarRegistroInvalido(dto, "MAPEAMENTO_INVALIDO", e.getMessage());
                log.warn("âš ï¸ LocalizaÃ§Ã£o de Carga invÃ¡lida descartada: {}", e.getMessage());
            }
        }
        if (registrosInvalidos > 0) {
            log.warn("âš ï¸ {} registro(s) invÃ¡lido(s) descartado(s) em {}", registrosInvalidos, getEntityName());
        }
        if (entities.isEmpty()) {
            return new SaveResult(0, 0, registrosInvalidos);
        }
        
        // Deduplicar antes de salvar
        final List<LocalizacaoCargaEntity> entitiesUnicos = Deduplicator.deduplicarLocalizacoes(entities);
        final int totalUnicos = entitiesUnicos.size();
        
        if (entities.size() != entitiesUnicos.size()) {
            final int duplicadosRemovidos = entities.size() - entitiesUnicos.size();
            log.warn(ConstantesExtracao.MSG_LOG_DUPLICADOS_REMOVIDOS, duplicadosRemovidos);
        }
        
        final int registrosSalvos = repository.salvar(entitiesUnicos);
        return new SaveResult(registrosSalvos, totalUnicos, registrosInvalidos);
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

    private void auditarRegistroInvalido(final LocalizacaoCargaDTO dto,
                                         final String reasonCode,
                                         final String detalhe) {
        final String chaveReferencia = dto != null && dto.getSequenceNumber() != null
            ? String.valueOf(dto.getSequenceNumber())
            : null;
        invalidRecordAuditRepository.registrarRegistroInvalido(
            getEntityName(),
            reasonCode,
            detalhe,
            chaveReferencia,
            MapperUtil.toJson(dto)
        );
    }
}

