package br.com.extrator.modelo.rest.ocorrencias;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import br.com.extrator.db.entity.OcorrenciaEntity;

/**
 * Mapper (Tradutor) que transforma o DTO aninhado de Ocorrência em uma
 * Entidade "achatada" (flattened), pronta para a persistência.
 * Ele promove campos-chave de objetos aninhados para o nível superior da Entidade
 * e armazena o DTO original completo como metadados.
 */
public class OcorrenciaMapper {
    private static final Logger logger = LoggerFactory.getLogger(OcorrenciaMapper.class);
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
    public OcorrenciaEntity toEntity(final OcorrenciaDTO dto) {
        if (dto == null) {
            return null;
        }

        final OcorrenciaEntity entity = new OcorrenciaEntity();

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
            if (dto.getOccurrenceAt() != null && !dto.getOccurrenceAt().trim().isEmpty()) {
                // OffsetDateTime.parse é ideal para strings de data/hora com timezone (ISO 8601)
                entity.setOccurrenceAt(OffsetDateTime.parse(dto.getOccurrenceAt()));
            }
        } catch (final Exception e) {
            logger.error("❌ Erro ao converter data para ocorrência ID {}: occurrenceAt='{}' - {}", 
                dto.getId(), dto.getOccurrenceAt(), e.getMessage());
            logger.debug("Stack trace completo:", e);
        }

        // 4. Empacotamento de todos os metadados
        try {
            // Serializa o DTO original completo para a coluna de metadados
            final String metadata = objectMapper.writeValueAsString(dto);
            entity.setMetadata(metadata);
        } catch (final JsonProcessingException e) {
            logger.error("❌ CRÍTICO: Falha ao serializar metadados para ocorrência ID {}: {}", 
                dto.getId(), e.getMessage(), e);
            // Tenta serializar versão simplificada como fallback
            entity.setMetadata(String.format("{\"error\":\"Serialization failed\",\"id\":%d}", dto.getId()));
        }

        return entity;
    }
}
