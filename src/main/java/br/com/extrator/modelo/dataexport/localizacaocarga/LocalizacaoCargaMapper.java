package br.com.extrator.modelo.dataexport.localizacaocarga;

import br.com.extrator.db.entity.LocalizacaoCargaEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapper (Tradutor) que transforma o LocalizacaoCargaDTO (dados brutos da API Data Export)
 * em uma LocalizacaoCargaEntity (pronta para o banco de dados).
 *
 * @author Lucas
 */
public class LocalizacaoCargaMapper {

    private static final Logger logger = LoggerFactory.getLogger(LocalizacaoCargaMapper.class);
    private final ObjectMapper objectMapper;

    public LocalizacaoCargaMapper() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Converte o DTO de Localização de Carga em uma Entidade.
     * @param dto O objeto DTO com os dados do registro.
     * @return Um objeto LocalizacaoCargaEntity pronto para ser salvo.
     */
    public LocalizacaoCargaEntity toEntity(LocalizacaoCargaDTO dto) {
        if (dto == null) {
            return null;
        }

        LocalizacaoCargaEntity entity = new LocalizacaoCargaEntity();

        // 1. Mapeamento dos campos essenciais
        entity.setSequenceNumber(dto.getSequenceNumber());
        entity.setStatus(dto.getStatus());
        entity.setOriginLocationName(dto.getOriginLocationName());
        entity.setDestinationLocationName(dto.getDestinationLocationName());

        // 2. Conversão segura de tipos de dados
        try {
            if (dto.getServiceAt() != null && !dto.getServiceAt().trim().isEmpty()) {
                entity.setServiceAt(OffsetDateTime.parse(dto.getServiceAt()));
            }
            if (dto.getPredictedDeliveryAt() != null && !dto.getPredictedDeliveryAt().trim().isEmpty()) {
                entity.setPredictedDeliveryAt(OffsetDateTime.parse(dto.getPredictedDeliveryAt()));
            }
            if (dto.getTotalValue() != null && !dto.getTotalValue().trim().isEmpty()) {
                entity.setTotalValue(new BigDecimal(dto.getTotalValue()));
            }
        } catch (DateTimeParseException | NumberFormatException e) {
            logger.error("❌ Erro ao converter dados para localização carga {}: serviceAt='{}', predictedDeliveryAt='{}', totalValue='{}' - {}", 
                dto.getSequenceNumber(), dto.getServiceAt(), dto.getPredictedDeliveryAt(), dto.getTotalValue(), e.getMessage());
            logger.debug("Stack trace completo:", e);
        }

        // 3. Empacotamento de todos os metadados
        try {
            String metadata = objectMapper.writeValueAsString(dto.getAllProperties());
            entity.setMetadata(metadata);
        } catch (JsonProcessingException e) {
            logger.error("❌ CRÍTICO: Falha ao serializar metadados para localização carga {}: {}", 
                dto.getSequenceNumber(), e.getMessage(), e);
            entity.setMetadata(String.format("{\"error\":\"Serialization failed\",\"sequence_number\":%d}", dto.getSequenceNumber()));
        }

        return entity;
    }
}
