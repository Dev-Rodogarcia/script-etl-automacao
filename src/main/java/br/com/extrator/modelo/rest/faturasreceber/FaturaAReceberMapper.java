package br.com.extrator.modelo.rest.faturasreceber;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.extrator.db.entity.FaturaAReceberEntity;

/**
 * Mapper (Tradutor) responsável por transformar o DTO de Fatura a Receber
 * em uma entidade pronta para persistência no banco de dados.
 * A lógica é simplificada, pois todos os dados necessários já estão no DTO.
 * A principal função é converter tipos e empacotar todos os dados
 * em uma única coluna de metadados para garantir resiliência e completude.
 */
public class FaturaAReceberMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converte o DTO completo em uma única Entidade.
     * @param dto O objeto DTO com todos os dados da fatura.
     * @return Um objeto FaturaAReceberEntity pronto para ser salvo pelo Repository.
     */
    public FaturaAReceberEntity toEntity(final FaturaAReceberDTO dto) {
        if (dto == null) {
            return null;
        }

        final FaturaAReceberEntity entity = new FaturaAReceberEntity();

        // 1. Mapeamento dos campos-chave e essenciais
        entity.setId(dto.getId());
        entity.setDocumentNumber(dto.getDocument());
        entity.setInvoiceType(dto.getType());

        if (dto.getCustomer() != null) {
            entity.setCustomerCnpj(dto.getCustomer().getCnpj());
            entity.setCustomerName(dto.getCustomer().getName());
        }

        // 2. Conversão segura de tipos (String para tipos do banco)
        try {
            if (dto.getIssueDate() != null) {
                entity.setIssueDate(LocalDate.parse(dto.getIssueDate()));
            }
            if (dto.getDueDate() != null) {
                entity.setDueDate(LocalDate.parse(dto.getDueDate()));
            }
            if (dto.getValue() != null) {
                entity.setTotalValue(new BigDecimal(dto.getValue()));
            }
        } catch (final Exception e) {
            System.err.println("Erro ao converter dados para a fatura ID: " + dto.getId() + " - " + e.getMessage());
        }

        // 3. Empacotamento de todos os metadados
        try {
            // Serializa o DTO completo, incluindo os 'otherProperties' (como installments),
            // para garantir que 100% dos dados sejam salvos em uma única coluna.
            final String metadata = objectMapper.writeValueAsString(dto);
            entity.setMetadata(metadata);
        } catch (final JsonProcessingException e) {
            entity.setMetadata("{\"error\":\"Falha ao serializar metadados\"}");
        }

        return entity;
    }
}