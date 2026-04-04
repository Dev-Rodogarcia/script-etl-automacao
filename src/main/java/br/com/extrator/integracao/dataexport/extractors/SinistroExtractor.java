package br.com.extrator.integracao.dataexport.extractors;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.extrator.dominio.dataexport.sinistros.SinistroDTO;
import br.com.extrator.integracao.ClienteApiDataExport;
import br.com.extrator.integracao.ResultadoExtracao;
import br.com.extrator.integracao.comum.ConstantesExtracao;
import br.com.extrator.integracao.comum.DataExportEntityExtractor;
import br.com.extrator.integracao.mapeamento.dataexport.sinistros.SinistroMapper;
import br.com.extrator.persistencia.entidade.SinistroEntity;
import br.com.extrator.persistencia.repositorio.InvalidRecordAuditRepository;
import br.com.extrator.persistencia.repositorio.SinistroRepository;
import br.com.extrator.suporte.console.LoggerConsole;
import br.com.extrator.suporte.mapeamento.MapperUtil;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

public class SinistroExtractor implements DataExportEntityExtractor<SinistroDTO> {

    private final ClienteApiDataExport apiClient;
    private final SinistroRepository repository;
    private final SinistroMapper mapper;
    private final LoggerConsole log;
    private final InvalidRecordAuditRepository invalidRecordAuditRepository;

    public SinistroExtractor(final ClienteApiDataExport apiClient,
                             final SinistroRepository repository,
                             final SinistroMapper mapper,
                             final LoggerConsole log) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.mapper = mapper;
        this.log = log;
        this.invalidRecordAuditRepository = new InvalidRecordAuditRepository();
    }

    @Override
    public ResultadoExtracao<SinistroDTO> extract(final LocalDate dataInicio, final LocalDate dataFim) {
        if (dataInicio != null) {
            final LocalDate fim = dataFim != null ? dataFim : dataInicio;
            return apiClient.buscarSinistros(dataInicio, fim);
        }
        return apiClient.buscarSinistros();
    }

    @Override
    public SaveResult saveWithDeduplication(final List<SinistroDTO> dtos) throws SQLException {
        if (dtos == null || dtos.isEmpty()) {
            return new SaveResult(0, 0);
        }

        final List<SinistroEntity> entities = new ArrayList<>();
        int invalidos = 0;
        for (final SinistroDTO dto : dtos) {
            try {
                final SinistroEntity entity = mapper.toEntity(dto);
                if (entity != null) {
                    entities.add(entity);
                } else {
                    invalidos++;
                    auditarRegistroInvalido(dto, "MAPPER_RETORNOU_NULL", "Mapper retornou entidade nula.");
                }
            } catch (final RuntimeException e) {
                invalidos++;
                auditarRegistroInvalido(dto, "MAPEAMENTO_INVALIDO", e.getMessage());
                log.warn("⚠️ Sinistro invalido descartado: {}", e.getMessage());
            }
        }
        if (entities.isEmpty()) {
            return new SaveResult(0, 0, invalidos);
        }

        final List<SinistroEntity> unicos = deduplicar(entities);
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
        return ConstantesEntidades.SINISTROS;
    }

    @Override
    public String getEmoji() {
        return ConstantesExtracao.EMOJI_SINISTROS;
    }

    @Override
    public boolean permiteConcluirComInvalidosAuditados() {
        return true;
    }

    private List<SinistroEntity> deduplicar(final List<SinistroEntity> entities) {
        final Map<String, SinistroEntity> unicos = new LinkedHashMap<>();
        for (final SinistroEntity entity : entities) {
            final String chave = entity.getIdentificadorUnico();
            final SinistroEntity atual = unicos.get(chave);
            if (atual == null || ehMaisRecente(entity, atual)) {
                unicos.put(chave, entity);
            }
        }
        return new ArrayList<>(unicos.values());
    }

    private boolean ehMaisRecente(final SinistroEntity candidato, final SinistroEntity atual) {
        final OffsetDateTime candidatoFreshness = candidato.getTreatmentAt();
        final OffsetDateTime atualFreshness = atual.getTreatmentAt();
        if (candidatoFreshness == null) {
            return false;
        }
        if (atualFreshness == null) {
            return true;
        }
        return candidatoFreshness.isAfter(atualFreshness);
    }

    private void auditarRegistroInvalido(final SinistroDTO dto,
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
