package br.com.extrator.modelo.rest.ocorrencias;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import br.com.extrator.db.entity.OcorrenciaEntity;

import java.time.OffsetDateTime;

/**
 * Mapper (Tradutor) que transforma o DTO aninhado de Ocorrência em uma
 * Entidade "achatada" (flattened), pronta para a persistência.
 * Ele promove campos-chave de objetos aninhados para o nível superior da Entidade
 * e armazena o DTO original completo como metadados.
 */
public class OcorrenciaMapper {
    private final ObjectMapper objectMapper;

    public OcorrenciaMapper() {
        this.objectMapper = new ObjectMapper();
        // Registra o módulo para lidar com tipos de data/hora do Java 8+, como OffsetDateTime
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Converte o DTO de Ocorrência em uma Entidade.
     * @param dto O objeto DTO com os dados da ocorrência.
     * @return Um objeto OcorrenciaEntity pronto para ser salvo.
     */
    public OcorrenciaEntity toEntity(OcorrenciaDTO dto) {
        if (dto == null) {
            return null;
        }

        OcorrenciaEntity entity = new OcorrenciaEntity();

        // 1. Mapeamento do ID principal
        entity.setId(dto.getId());

        // 2. Extração segura de campos de objetos aninhados
        if (dto.getOccurrenceDetails() != null) {
            entity.setOccurrenceCode(dto.getOccurrenceDetails().getCode());
            entity.setOccurrenceDescription(dto.getOccurrenceDetails().getDescription());
        }
        if (dto.getFreight() != null) {
            entity.setFreightId(dto.getFreight().getId());
            entity.setCteKey(dto.getFreight().getCteKey());
        }
        if (dto.getInvoice() != null) {
            entity.setInvoiceId(dto.getInvoice().getId());
            entity.setInvoiceKey(dto.getInvoice().getKey());
        }

        // 3. Conversão e tratamento de tipos de data/hora com fuso horário
        try {
            if (dto.getOccurrenceAt() != null) {
                // OffsetDateTime.parse é ideal para strings de data/hora com timezone (ISO 8601)
                entity.setOccurrenceAt(OffsetDateTime.parse(dto.getOccurrenceAt()));
            }
        } catch (Exception e) {
            System.err.println("Erro ao converter data para a ocorrência ID: " + dto.getId() + " - " + e.getMessage());
        }

        // 4. Empacotamento de todos os metadados
        try {
            // Serializa o DTO original completo para a coluna de metadados
            String metadata = objectMapper.writeValueAsString(dto);
            entity.setMetadata(metadata);
        } catch (JsonProcessingException e) {
            entity.setMetadata("{\"error\":\"Falha ao serializar metadados\"}");
        }

        return entity;
    }
}
