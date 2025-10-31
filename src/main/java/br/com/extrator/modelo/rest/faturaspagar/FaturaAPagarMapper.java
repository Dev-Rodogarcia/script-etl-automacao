package br.com.extrator.modelo.rest.faturaspagar;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.extrator.db.entity.FaturaAPagarEntity;

/**
 * Mapper (Tradutor) responsável por transformar os dados brutos da API
 * em uma entidade pronta para persistência no banco de dados.
 * Sua principal responsabilidade é combinar os dados do DTO do cabeçalho da fatura
 * com o JSON bruto de seus títulos/parcelas.
 */
public class FaturaAPagarMapper {

    private static final Logger logger = LoggerFactory.getLogger(FaturaAPagarMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converte o DTO do cabeçalho e o JSON das parcelas em uma única Entidade.
     * @param dto O objeto DTO com os dados do cabeçalho da fatura.
     * @param installmentsJson A String JSON bruta contendo a lista de títulos/parcelas.
     * @return Um objeto FaturaAPagarEntity pronto para ser salvo pelo Repository.
     */
    public FaturaAPagarEntity toEntity(final FaturaAPagarDTO dto, final String installmentsJson) {
        if (dto == null) {
            return null;
        }

        final FaturaAPagarEntity entity = new FaturaAPagarEntity();

        // 1. Mapeamento dos campos-chave e essenciais
        entity.setId(dto.getId());
        entity.setDocumentNumber(dto.getDocument());
        entity.setInvoiceType(dto.getType());

        if (dto.getReceiver() != null) {
            entity.setReceiverCnpj(dto.getReceiver().getCnpj());
            entity.setReceiverName(dto.getReceiver().getName());
        }

        // 2. Conversão segura de tipos (String para tipos do banco)
        try {
            if (dto.getIssueDate() != null && !dto.getIssueDate().trim().isEmpty()) {
                entity.setIssueDate(LocalDate.parse(dto.getIssueDate()));
            }
            if (dto.getDueDate() != null && !dto.getDueDate().trim().isEmpty()) {
                entity.setDueDate(LocalDate.parse(dto.getDueDate()));
            }
            if (dto.getValue() != null && !dto.getValue().trim().isEmpty()) {
                entity.setTotalValue(new BigDecimal(dto.getValue()));
            }
        } catch (final Exception e) {
            logger.error("❌ Erro ao converter dados para fatura a pagar ID {}: issueDate='{}', dueDate='{}', value='{}' - {}", 
                dto.getId(), dto.getIssueDate(), dto.getDueDate(), dto.getValue(), e.getMessage());
            logger.debug("Stack trace completo:", e);
            // Mantém NULL mas REGISTRA o problema
        }

        // 3. Empacotamento dos metadados
        try {
            // Serializa o DTO completo para garantir que todos os dados do cabeçalho sejam salvos
            final String headerMetadata = objectMapper.writeValueAsString(dto);
            entity.setHeaderMetadata(headerMetadata);
        } catch (final JsonProcessingException e) {
            logger.error("❌ CRÍTICO: Falha ao serializar header metadata para fatura a pagar ID {}: {}", 
                dto.getId(), e.getMessage(), e);
            entity.setHeaderMetadata(String.format("{\"error\":\"Serialization failed\",\"id\":%d}", dto.getId()));
        }

        // Armazena o JSON bruto dos títulos/parcelas diretamente (com validação)
        if (installmentsJson != null && !installmentsJson.trim().isEmpty()) {
            entity.setInstallmentsMetadata(installmentsJson);
        } else {
            logger.warn("⚠️ installmentsJson está nulo ou vazio para fatura a pagar ID {}", dto.getId());
            entity.setInstallmentsMetadata("[]"); // Array vazio como fallback
        }

        return entity;
    }
}