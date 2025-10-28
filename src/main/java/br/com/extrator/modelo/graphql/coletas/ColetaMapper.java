package br.com.extrator.modelo.graphql.coletas;

import br.com.extrator.db.entity.ColetaEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;

/**
 * Mapper (Tradutor) que transforma o ColetaNodeDTO (dados brutos do GraphQL)
 * em uma ColetaEntity (pronta para o banco de dados).
 * Realiza a conversão de tipos (String para LocalDate) e garante que
 * 100% dos dados originais sejam preservados na coluna de metadados.
 */
public class ColetaMapper {

    private final ObjectMapper objectMapper;

    public ColetaMapper() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Converte o DTO de Coleta em uma Entidade.
     * @param dto O objeto DTO com os dados da coleta.
     * @return Um objeto ColetaEntity pronto para ser salvo.
     */
    public ColetaEntity toEntity(ColetaNodeDTO dto) {
        if (dto == null) {
            return null;
        }

        ColetaEntity entity = new ColetaEntity();

        // 1. Mapeamento dos campos essenciais
        entity.setId(dto.getId());
        entity.setSequenceCode(dto.getSequenceCode());
        entity.setStatus(dto.getStatus());
        entity.setTotalValue(dto.getInvoicesValue());
        entity.setTotalWeight(dto.getInvoicesWeight());
        entity.setTotalVolumes(dto.getInvoicesVolumes());

        // 2. Conversão segura de tipos de data
        try {
            if (dto.getRequestDate() != null) {
                entity.setRequestDate(LocalDate.parse(dto.getRequestDate()));
            }
            if (dto.getServiceDate() != null) {
                entity.setServiceDate(LocalDate.parse(dto.getServiceDate()));
            }
        } catch (Exception e) {
            // Logar o erro de parsing, se necessário
            System.err.println("Erro ao converter data para a coleta ID: " + dto.getId() + " - " + e.getMessage());
        }

        // 3. Empacotamento de todos os metadados
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
