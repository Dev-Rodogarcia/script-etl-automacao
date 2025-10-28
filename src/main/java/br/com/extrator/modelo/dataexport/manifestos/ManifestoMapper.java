package br.com.extrator.modelo.dataexport.manifestos;

import br.com.extrator.db.entity.ManifestoEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
            if (dto.getCreatedAt() != null) {
                entity.setCreatedAt(OffsetDateTime.parse(dto.getCreatedAt()));
            }
            if (dto.getDeparturedAt() != null) {
                entity.setDeparturedAt(OffsetDateTime.parse(dto.getDeparturedAt()));
            }
            if (dto.getFinishedAt() != null) {
                entity.setFinishedAt(OffsetDateTime.parse(dto.getFinishedAt()));
            }
            if (dto.getTotalCost() != null) {
                entity.setTotalCost(new BigDecimal(dto.getTotalCost()));
            }
        } catch (DateTimeParseException | NumberFormatException e) {
            System.err.println("Erro ao converter dados para o manifesto: " + dto.getSequenceCode() + " - " + e.getMessage());
        }

        // 3. Empacotamento de todos os metadados
        try {
            String metadata = objectMapper.writeValueAsString(dto.getAllProperties());
            entity.setMetadata(metadata);
        } catch (JsonProcessingException e) {
            entity.setMetadata("{\"error\":\"Falha ao serializar metadados\"}");
        }

        return entity;
    }
}
