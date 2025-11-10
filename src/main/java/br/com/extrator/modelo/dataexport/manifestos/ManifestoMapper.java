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

        // 1. Mapeamento dos campos essenciais conforme docs/descobertas-endpoints/manifestos.md
        entity.setSequenceCode(dto.getSequenceCode());
        entity.setStatus(dto.getStatus());
        entity.setBranchNickname(dto.getBranchNickname());
        entity.setMdfeNumber(dto.getMdfeNumber());
        entity.setMdfeKey(dto.getMdfeKey());
        entity.setMdfeStatus(dto.getMdfeStatus());
        entity.setDistributionPole(dto.getDistributionPole());
        entity.setClassification(dto.getClassification());
        entity.setVehiclePlate(dto.getVehiclePlate());
        entity.setVehicleType(dto.getVehicleType());
        entity.setVehicleOwner(dto.getVehicleOwner());
        entity.setDriverName(dto.getDriverName());
        entity.setVehicleDepartureKm(dto.getVehicleDepartureKm());
        entity.setClosingKm(dto.getClosingKm());
        entity.setTraveledKm(dto.getTraveledKm());
        entity.setInvoicesCount(dto.getInvoicesCount());
        entity.setInvoicesVolumes(dto.getInvoicesVolumes());
        entity.setInvoicesWeight(dto.getInvoicesWeight());
        entity.setTotalTaxedWeight(dto.getTotalTaxedWeight());
        entity.setTotalCubicVolume(dto.getTotalCubicVolume());
        entity.setInvoicesValue(dto.getInvoicesValue());
        entity.setManifestFreightsTotal(dto.getManifestFreightsTotal());
        entity.setPickSequenceCode(dto.getPickSequenceCode());
        entity.setContractNumber(dto.getContractNumber());
        entity.setDailySubtotal(dto.getDailySubtotal());
        entity.setOperationalExpensesTotal(dto.getOperationalExpensesTotal());
        entity.setInssValue(dto.getInssValue());
        entity.setSestSenatValue(dto.getSestSenatValue());
        entity.setIrValue(dto.getIrValue());
        entity.setPayingTotal(dto.getPayingTotal());
        entity.setCreationUserName(dto.getCreationUserName());
        entity.setAdjustmentUserName(dto.getAdjustmentUserName());

        // 2. Conversão segura de tipos de dados
        try {
            if (dto.getCreatedAt() != null && !dto.getCreatedAt().trim().isEmpty()) {
                entity.setCreatedAt(OffsetDateTime.parse(dto.getCreatedAt()));
            }
            if (dto.getDeparturedAt() != null && !dto.getDeparturedAt().trim().isEmpty()) {
                entity.setDeparturedAt(OffsetDateTime.parse(dto.getDeparturedAt()));
            }
            if (dto.getClosedAt() != null && !dto.getClosedAt().trim().isEmpty()) {
                entity.setClosedAt(OffsetDateTime.parse(dto.getClosedAt()));
            }
            if (dto.getFinishedAt() != null && !dto.getFinishedAt().trim().isEmpty()) {
                entity.setFinishedAt(OffsetDateTime.parse(dto.getFinishedAt()));
            }
            if (dto.getTotalCost() != null && !dto.getTotalCost().trim().isEmpty()) {
                entity.setTotalCost(new BigDecimal(dto.getTotalCost()));
            }
        } catch (DateTimeParseException | NumberFormatException e) {
            logger.error("❌ Erro ao converter dados para manifesto {}: createdAt='{}', departuredAt='{}', closedAt='{}', finishedAt='{}', totalCost='{}' - {}", 
                dto.getSequenceCode(), dto.getCreatedAt(), dto.getDeparturedAt(), dto.getClosedAt(), dto.getFinishedAt(), dto.getTotalCost(), e.getMessage());
            logger.debug("Stack trace completo:", e);
        }

        // 3. Empacotamento de todos os metadados
        try {
            String metadata = objectMapper.writeValueAsString(dto.getAllProperties());
            entity.setMetadata(metadata);
            
            // 4. Calcular identificador único APÓS definir metadata
            // Isso permite usar pick_sequence_code ou hash do metadata
            entity.calcularIdentificadorUnico();
            
        } catch (JsonProcessingException e) {
            logger.error("❌ CRÍTICO: Falha ao serializar metadados para manifesto {}: {}", 
                dto.getSequenceCode(), e.getMessage(), e);
            entity.setMetadata(String.format("{\"error\":\"Serialization failed\",\"sequence_code\":%d}", dto.getSequenceCode()));
            // Tentar calcular identificador mesmo com erro (usará fallback)
            entity.calcularIdentificadorUnico();
        }

        return entity;
    }
}
