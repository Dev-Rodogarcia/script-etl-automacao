package br.com.extrator.modelo.dataexport.localizacaocarga;

import br.com.extrator.db.entity.LocalizacaoCargaEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Mapper (Tradutor) que transforma o LocalizacaoCargaDTO (dados brutos do Data Export)
 * em uma LocalizacaoCargaEntity (pronta para o banco de dados).
 * É responsável pela conversão de tipos (String para BigDecimal/OffsetDateTime)
 * e pela serialização de todos os dados na coluna de metadados.
 */
public class LocalizacaoCargaMapper {

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
            if (dto.getServiceAt() != null) {
                entity.setServiceAt(OffsetDateTime.parse(dto.getServiceAt()));
            }
            if (dto.getPredictedDeliveryAt() != null) {
                entity.setPredictedDeliveryAt(OffsetDateTime.parse(dto.getPredictedDeliveryAt()));
            }
            if (dto.getTotalValue() != null) {
                entity.setTotalValue(new BigDecimal(dto.getTotalValue()));
            }
        } catch (DateTimeParseException | NumberFormatException e) {
            System.err.println("Erro ao converter dados para o registro: " + dto.getSequenceNumber() + " - " + e.getMessage());
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
