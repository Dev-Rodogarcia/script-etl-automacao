package br.com.extrator.modelo.dataexport.cotacao;

import br.com.extrator.db.entity.CotacaoEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapper (Tradutor) que transforma o CotacaoDTO (dados brutos da API Data Export)
 * em uma CotacaoEntity (pronta para o banco de dados).
 * É responsável pela conversão de tipos (String para BigDecimal/OffsetDateTime)
 * e pela serialização de todos os dados na coluna de metadados.
 *
 * @author Lucas
 */
public class CotacaoMapper {

    private static final Logger logger = LoggerFactory.getLogger(CotacaoMapper.class);
    private final ObjectMapper objectMapper;

    public CotacaoMapper() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Converte o DTO de Cotação em uma Entidade.
     * @param dto O objeto DTO com os dados da cotação.
     * @return Um objeto CotacaoEntity pronto para ser salvo.
     */
    public CotacaoEntity toEntity(CotacaoDTO dto) {
        if (dto == null) {
            return null;
        }

        CotacaoEntity entity = new CotacaoEntity();

        // 1. Mapeamento dos campos essenciais
        entity.setSequenceCode(dto.getSequenceCode());
        entity.setOriginCity(dto.getOriginCity());
        entity.setOriginState(dto.getOriginState());
        entity.setDestinationCity(dto.getDestinationCity());
        entity.setDestinationState(dto.getDestinationState());
        entity.setCustomerDoc(dto.getCustomerDocument());

        // 2. Conversão segura de tipos de dados (String para tipos específicos)
        try {
            if (dto.getRequestedAt() != null && !dto.getRequestedAt().trim().isEmpty()) {
                entity.setRequestedAt(OffsetDateTime.parse(dto.getRequestedAt()));
            }
            if (dto.getTotalValue() != null && !dto.getTotalValue().trim().isEmpty()) {
                entity.setTotalValue(new BigDecimal(dto.getTotalValue()));
            }
            if (dto.getTaxedWeight() != null && !dto.getTaxedWeight().trim().isEmpty()) {
                entity.setTaxedWeight(new BigDecimal(dto.getTaxedWeight()));
            }
            if (dto.getInvoicesValue() != null && !dto.getInvoicesValue().trim().isEmpty()) {
                entity.setInvoicesValue(new BigDecimal(dto.getInvoicesValue()));
            }
        } catch (DateTimeParseException | NumberFormatException e) {
            logger.error("❌ Erro ao converter dados para cotação {}: requestedAt='{}', totalValue='{}', taxedWeight='{}', invoicesValue='{}' - {}", 
                dto.getSequenceCode(), dto.getRequestedAt(), dto.getTotalValue(), dto.getTaxedWeight(), dto.getInvoicesValue(), e.getMessage());
            logger.debug("Stack trace completo:", e);
        }

        // 3. Empacotamento de todos os metadados
        try {
            // Serializa o mapa completo que inclui campos explícitos e o "resto"
            String metadata = objectMapper.writeValueAsString(dto.getAllProperties());
            entity.setMetadata(metadata);
        } catch (JsonProcessingException e) {
            logger.error("❌ CRÍTICO: Falha ao serializar metadados para cotação {}: {}", 
                dto.getSequenceCode(), e.getMessage(), e);
            entity.setMetadata(String.format("{\"error\":\"Serialization failed\",\"sequence_code\":%d}", dto.getSequenceCode()));
        }

        return entity;
    }
}
