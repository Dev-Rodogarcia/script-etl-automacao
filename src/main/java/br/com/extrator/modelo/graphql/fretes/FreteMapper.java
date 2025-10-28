package br.com.extrator.modelo.graphql.fretes;

import br.com.extrator.db.entity.FreteEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Mapper (Tradutor) que transforma o FreteNodeDTO (dados brutos do GraphQL)
 * em uma FreteEntity (pronta para o banco de dados).
 * Converte tipos de data/hora e preserva 100% dos dados originais
 * na coluna de metadados.
 */
public class FreteMapper {

    private final ObjectMapper objectMapper;

    public FreteMapper() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Converte o DTO de Frete em uma Entidade.
     * @param dto O objeto DTO com os dados do frete.
     * @return Um objeto FreteEntity pronto para ser salvo.
     */
    public FreteEntity toEntity(FreteNodeDTO dto) {
        if (dto == null) {
            return null;
        }

        FreteEntity entity = new FreteEntity();

        // 1. Mapeamento dos campos essenciais
        entity.setId(dto.getId());
        entity.setStatus(dto.getStatus());
        entity.setModal(dto.getModal());
        entity.setTipoFrete(dto.getType());
        entity.setValorTotal(dto.getTotalValue());
        entity.setValorNotas(dto.getInvoicesValue());
        entity.setPesoNotas(dto.getInvoicesWeight());
        entity.setIdCorporacao(dto.getCorporationId());
        entity.setIdCidadeDestino(dto.getDestinationCityId());

        // 2. Conversão segura de tipos de data e hora
        try {
            if (dto.getServiceAt() != null) {
                entity.setServicoEm(OffsetDateTime.parse(dto.getServiceAt()));
            }
            if (dto.getCreatedAt() != null) {
                entity.setCriadoEm(OffsetDateTime.parse(dto.getCreatedAt()));
            }
            if (dto.getDeliveryPredictionDate() != null) {
                entity.setDataPrevisaoEntrega(LocalDate.parse(dto.getDeliveryPredictionDate()));
            }
        } catch (DateTimeParseException e) {
            System.err.println("Erro ao converter data para o frete ID: " + dto.getId() + " - " + e.getMessage());
        }

        // 3. Empacotamento de todos os metadados
        try {
            String metadata = objectMapper.writeValueAsString(dto);
            entity.setMetadata(metadata);
        } catch (JsonProcessingException e) {
            entity.setMetadata("{\"error\":\"Falha ao serializar metadados\"}");
        }

        return entity;
    }
}