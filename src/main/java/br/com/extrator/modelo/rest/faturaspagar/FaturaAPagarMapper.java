package br.com.extrator.modelo.rest.faturaspagar;

import java.math.BigDecimal;
import java.time.LocalDate;

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
            // Aqui você pode logar um erro de parsing, se necessário
            System.err.println("Erro ao converter dados para a fatura ID: " + dto.getId() + " - " + e.getMessage());
        }

        // 3. Empacotamento dos metadados
        try {
            // Serializa o DTO completo para garantir que todos os dados do cabeçalho sejam salvos
            final String headerMetadata = objectMapper.writeValueAsString(dto);
            entity.setHeaderMetadata(headerMetadata);
        } catch (final JsonProcessingException e) {
            entity.setHeaderMetadata("{\"error\":\"Falha ao serializar metadados do cabeçalho\"}");
        }

        // Armazena o JSON bruto dos títulos/parcelas diretamente
        entity.setInstallmentsMetadata(installmentsJson);

        return entity;
    }
}