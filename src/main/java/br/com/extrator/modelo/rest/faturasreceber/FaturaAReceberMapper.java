package br.com.extrator.modelo.rest.faturasreceber;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(FaturaAReceberMapper.class);
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
            logger.error("❌ Erro ao converter dados para fatura a receber ID {}: issueDate='{}', dueDate='{}', value='{}' - {}", 
                dto.getId(), dto.getIssueDate(), dto.getDueDate(), dto.getValue(), e.getMessage());
            logger.debug("Stack trace completo:", e);
            // Mantém NULL mas REGISTRA o problema
        }

        // 3. Empacotamento de todos os metadados
        try {
            // Serializa o DTO completo, incluindo os 'otherProperties' (como installments),
            // para garantir que 100% dos dados sejam salvos em uma única coluna.
            final String metadata = objectMapper.writeValueAsString(dto);
            entity.setMetadata(metadata);
        } catch (final JsonProcessingException e) {
            logger.error("❌ CRÍTICO: Falha ao serializar metadados para fatura a receber ID {}: {}", 
                dto.getId(), e.getMessage(), e);
            // Tenta serializar versão simplificada como fallback
            entity.setMetadata(String.format("{\"error\":\"Serialization failed\",\"id\":%d}", dto.getId()));
        }

        return entity;
    }
}