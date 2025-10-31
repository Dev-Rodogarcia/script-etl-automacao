package br.com.extrator.modelo.dataexport.manifestos;

import br.com.extrator.db.entity.ManifestoEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Mapper (Tradutor) que transforma o ManifestoDTO (dados brutos do Data Export)
 * em uma ManifestoEntity (pronta para o banco de dados).
 * É responsável pela conversão de tipos e pela serialização de todos os
 * dados na coluna de metadados.
 */
public class ManifestoMapper {

    private static final Logger logger = LoggerFactory.getLogger(ManifestoMapper.class);

    private final ObjectMapper objectMapper;

    public ManifestoMapper() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Converte o DTO de Manifesto em uma Entidade.
     * @param dto O objeto DTO com os dados do manifesto.
     * @return Um objeto ManifestoEntity pronto para ser salvo.
     */
    public ManifestoEntity toEntity(ManifestoDTO dto) {
        if (dto == null) {
            return null;
        }

        ManifestoEntity entity = new ManifestoEntity();

        // 1. Mapeamento dos campos essenciais
        entity.setSequenceCode(dto.getSequenceCode());
        entity.setStatus(dto.getStatus());
        entity.setTraveledKm(dto.getTraveledKm());
        entity.setVehiclePlate(dto.getVehiclePlate());
        entity.setDriverName(dto.getDriverName());
        entity.setOriginBranch(dto.getOriginBranch());
        entity.setMdfeStatus(dto.getMdfeStatus());

        // 2. Conversão segura de tipos de dados
        try {
            if (dto.getCreatedAt() != null && !dto.getCreatedAt().trim().isEmpty()) {
                entity.setCreatedAt(OffsetDateTime.parse(dto.getCreatedAt()));
            }
            if (dto.getDeparturedAt() != null && !dto.getDeparturedAt().trim().isEmpty()) {
                entity.setDeparturedAt(OffsetDateTime.parse(dto.getDeparturedAt()));
            }
            if (dto.getFinishedAt() != null && !dto.getFinishedAt().trim().isEmpty()) {
                entity.setFinishedAt(OffsetDateTime.parse(dto.getFinishedAt()));
            }
            if (dto.getTotalCost() != null && !dto.getTotalCost().trim().isEmpty()) {
                entity.setTotalCost(new BigDecimal(dto.getTotalCost()));
            }
        } catch (DateTimeParseException | NumberFormatException e) {
            logger.error("❌ Erro ao converter dados para manifesto {}: createdAt='{}', departuredAt='{}', finishedAt='{}', totalCost='{}' - {}", 
                dto.getSequenceCode(), dto.getCreatedAt(), dto.getDeparturedAt(), dto.getFinishedAt(), dto.getTotalCost(), e.getMessage());
            logger.debug("Stack trace completo:", e);
        }

        // 3. Empacotamento de todos os metadados
        try {
            String metadata = objectMapper.writeValueAsString(dto.getAllProperties());
            entity.setMetadata(metadata);
        } catch (JsonProcessingException e) {
            logger.error("❌ CRÍTICO: Falha ao serializar metadados para manifesto {}: {}", 
                dto.getSequenceCode(), e.getMessage(), e);
            entity.setMetadata(String.format("{\"error\":\"Serialization failed\",\"sequence_code\":%d}", dto.getSequenceCode()));
        }

        return entity;
    }
}
