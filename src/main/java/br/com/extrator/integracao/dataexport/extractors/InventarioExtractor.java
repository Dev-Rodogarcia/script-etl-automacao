package br.com.extrator.integracao.dataexport.extractors;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.extrator.dominio.dataexport.inventario.InventarioDTO;
import br.com.extrator.integracao.ClienteApiDataExport;
import br.com.extrator.integracao.ResultadoExtracao;
import br.com.extrator.integracao.comum.ConstantesExtracao;
import br.com.extrator.integracao.comum.DataExportEntityExtractor;
import br.com.extrator.integracao.mapeamento.dataexport.inventario.InventarioMapper;
import br.com.extrator.persistencia.entidade.InventarioEntity;
import br.com.extrator.persistencia.repositorio.InvalidRecordAuditRepository;
import br.com.extrator.persistencia.repositorio.InventarioRepository;
import br.com.extrator.suporte.console.LoggerConsole;
import br.com.extrator.suporte.mapeamento.MapperUtil;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

public class InventarioExtractor implements DataExportEntityExtractor<InventarioDTO> {

    private final ClienteApiDataExport apiClient;
    private final InventarioRepository repository;
    private final InventarioMapper mapper;
    private final LoggerConsole log;
    private final InvalidRecordAuditRepository invalidRecordAuditRepository;

    public InventarioExtractor(final ClienteApiDataExport apiClient,
                               final InventarioRepository repository,
                               final InventarioMapper mapper,
                               final LoggerConsole log) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.mapper = mapper;
        this.log = log;
        this.invalidRecordAuditRepository = new InvalidRecordAuditRepository();
    }

    @Override
    public ResultadoExtracao<InventarioDTO> extract(final LocalDate dataInicio, final LocalDate dataFim) {
        if (dataInicio != null) {
            final LocalDate fim = dataFim != null ? dataFim : dataInicio;
            return apiClient.buscarInventario(dataInicio, fim);
        }
        return apiClient.buscarInventario();
    }

    @Override
    public SaveResult saveWithDeduplication(final List<InventarioDTO> dtos) throws SQLException {
        if (dtos == null || dtos.isEmpty()) {
            return new SaveResult(0, 0);
        }

        final List<InventarioEntity> entities = new ArrayList<>();
        int invalidos = 0;
        for (final InventarioDTO dto : dtos) {
            try {
                final InventarioEntity entity = mapper.toEntity(dto);
                if (entity != null) {
                    entities.add(entity);
                } else {
                    invalidos++;
                    auditarRegistroInvalido(dto, "MAPPER_RETORNOU_NULL", "Mapper retornou entidade nula.");
                }
            } catch (final RuntimeException e) {
                invalidos++;
                auditarRegistroInvalido(dto, "MAPEAMENTO_INVALIDO", e.getMessage());
                log.warn("⚠️ Inventario invalido descartado: {}", e.getMessage());
            }
        }
        if (entities.isEmpty()) {
            return new SaveResult(0, 0, invalidos);
        }

        final List<InventarioEntity> unicos = deduplicar(entities);
        final int registrosSalvos = repository.salvar(unicos);
        return new SaveResult(
            registrosSalvos,
            unicos.size(),
            invalidos,
            repository.getUltimoResumoSalvamento().getRegistrosPersistidos(),
            repository.getUltimoResumoSalvamento().getRegistrosNoOpIdempotente()
        );
    }

    @Override
    public String getEntityName() {
        return ConstantesEntidades.INVENTARIO;
    }

    @Override
    public String getEmoji() {
        return ConstantesExtracao.EMOJI_INVENTARIO;
    }

    @Override
    public boolean permiteConcluirComInvalidosAuditados() {
        return true;
    }

    private List<InventarioEntity> deduplicar(final List<InventarioEntity> entities) {
        final Map<String, InventarioEntity> unicos = new LinkedHashMap<>();
        for (final InventarioEntity entity : entities) {
            final String chave = entity.getIdentificadorUnico();
            final InventarioEntity atual = unicos.get(chave);
            if (atual == null || ehMaisRecente(entity, atual)) {
                unicos.put(chave, entity);
            }
        }
        return new ArrayList<>(unicos.values());
    }

    private boolean ehMaisRecente(final InventarioEntity candidato, final InventarioEntity atual) {
        final OffsetDateTime candidatoFreshness =
            candidato.getPerformanceFinishedAt() != null ? candidato.getPerformanceFinishedAt()
                : candidato.getFinishedAt() != null ? candidato.getFinishedAt()
                : candidato.getStartedAt();
        final OffsetDateTime atualFreshness =
            atual.getPerformanceFinishedAt() != null ? atual.getPerformanceFinishedAt()
                : atual.getFinishedAt() != null ? atual.getFinishedAt()
                : atual.getStartedAt();
        if (candidatoFreshness == null) {
            return false;
        }
        if (atualFreshness == null) {
            return true;
        }
        return candidatoFreshness.isAfter(atualFreshness);
    }

    private void auditarRegistroInvalido(final InventarioDTO dto,
                                         final String reasonCode,
                                         final String detalhe) {
        final String chaveReferencia = dto != null && dto.getSequenceCode() != null
            ? String.valueOf(dto.getSequenceCode())
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
