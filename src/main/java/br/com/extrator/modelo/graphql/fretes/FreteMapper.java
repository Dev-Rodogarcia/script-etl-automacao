package br.com.extrator.modelo.graphql.fretes;

import br.com.extrator.db.entity.FreteEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(FreteMapper.class);

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

        // 1.1. Mapeamento dos campos expandidos (22 campos do CSV)
        if (dto.getPayer() != null) {
            entity.setPagadorId(dto.getPayer().getId());
            entity.setPagadorNome(dto.getPayer().getName());
        }

        if (dto.getSender() != null) {
            entity.setRemetenteId(dto.getSender().getId());
            entity.setRemetenteNome(dto.getSender().getName());
            if (dto.getSender().getMainAddress() != null && 
                dto.getSender().getMainAddress().getCity() != null) {
                entity.setOrigemCidade(dto.getSender().getMainAddress().getCity().getName());
                if (dto.getSender().getMainAddress().getCity().getState() != null) {
                    entity.setOrigemUf(dto.getSender().getMainAddress().getCity().getState().getCode());
                }
            }
        }

        if (dto.getReceiver() != null) {
            entity.setDestinatarioId(dto.getReceiver().getId());
            entity.setDestinatarioNome(dto.getReceiver().getName());
            if (dto.getReceiver().getMainAddress() != null && 
                dto.getReceiver().getMainAddress().getCity() != null) {
                entity.setDestinoCidade(dto.getReceiver().getMainAddress().getCity().getName());
                if (dto.getReceiver().getMainAddress().getCity().getState() != null) {
                    entity.setDestinoUf(dto.getReceiver().getMainAddress().getCity().getState().getCode());
                }
            }
        }

        // Mapear campos expandidos adicionais
        if (dto.getCorporation() != null) {
            entity.setFilialNome(dto.getCorporation().getName());
        }

        if (dto.getFreightInvoices() != null && !dto.getFreightInvoices().isEmpty()) {
            // Pegar primeira NF (ou concatenar todas)
            entity.setNumeroNotaFiscal(dto.getFreightInvoices().get(0).getNumber());
        }

        if (dto.getCustomerPriceTable() != null) {
            entity.setTabelaPrecoNome(dto.getCustomerPriceTable().getName());
        }

        if (dto.getFreightClassification() != null) {
            entity.setClassificacaoNome(dto.getFreightClassification().getName());
        }

        if (dto.getCostCenter() != null) {
            entity.setCentroCustoNome(dto.getCostCenter().getName());
        }

        if (dto.getUser() != null) {
            entity.setUsuarioNome(dto.getUser().getName());
        }

        // Mapear campos simples adicionais (22 campos do CSV)
        entity.setReferenceNumber(dto.getReferenceNumber());
        entity.setInvoicesTotalVolumes(dto.getInvoicesTotalVolumes());
        entity.setTaxedWeight(dto.getTaxedWeight());
        entity.setRealWeight(dto.getRealWeight());
        entity.setTotalCubicVolume(dto.getTotalCubicVolume());
        entity.setSubtotal(dto.getSubtotal());

        // 2. Conversão segura de tipos de data e hora
        try {
            if (dto.getServiceAt() != null && !dto.getServiceAt().trim().isEmpty()) {
                entity.setServicoEm(OffsetDateTime.parse(dto.getServiceAt()));
            }
            if (dto.getCreatedAt() != null && !dto.getCreatedAt().trim().isEmpty()) {
                entity.setCriadoEm(OffsetDateTime.parse(dto.getCreatedAt()));
            }
            if (dto.getDeliveryPredictionDate() != null && !dto.getDeliveryPredictionDate().trim().isEmpty()) {
                entity.setDataPrevisaoEntrega(LocalDate.parse(dto.getDeliveryPredictionDate()));
            }
        } catch (DateTimeParseException e) {
            logger.error("❌ Erro ao converter data para frete ID {}: serviceAt='{}', createdAt='{}', deliveryPredictionDate='{}' - {}", 
                dto.getId(), dto.getServiceAt(), dto.getCreatedAt(), dto.getDeliveryPredictionDate(), e.getMessage());
            logger.debug("Stack trace completo:", e);
        }

        // 3. Empacotamento de todos os metadados
        try {
            String metadata = objectMapper.writeValueAsString(dto);
            entity.setMetadata(metadata);
        } catch (JsonProcessingException e) {
            logger.error("❌ CRÍTICO: Falha ao serializar metadados para frete ID {}: {}", 
                dto.getId(), e.getMessage(), e);
            entity.setMetadata(String.format("{\"error\":\"Serialization failed\",\"id\":%d}", dto.getId()));
        }

        return entity;
    }
}