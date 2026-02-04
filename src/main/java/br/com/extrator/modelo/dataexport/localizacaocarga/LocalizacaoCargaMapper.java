package br.com.extrator.modelo.dataexport.localizacaocarga;

import br.com.extrator.db.entity.LocalizacaoCargaEntity;
import br.com.extrator.util.validacao.ValidadorDTO;
import br.com.extrator.util.validacao.ValidadorDTO.ResultadoValidacao;
import br.com.extrator.util.mapeamento.MapperUtil;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapper (Tradutor) que transforma o LocalizacaoCargaDTO (dados brutos da API Data Export)
 * em uma LocalizacaoCargaEntity (pronta para o banco de dados).
 *
 * @author Lucas
 */
public class LocalizacaoCargaMapper {

    private static final Logger logger = LoggerFactory.getLogger(LocalizacaoCargaMapper.class);

    public LocalizacaoCargaMapper() {
        // Usando MapperUtil para ObjectMapper compartilhado
    }

    /**
     * Converte o DTO de Localização de Carga em uma Entidade.
     * PROBLEMA #6 CORRIGIDO: Adicionada validação de campos críticos.
     * 
     * @param dto O objeto DTO com os dados do registro.
     * @return Um objeto LocalizacaoCargaEntity pronto para ser salvo.
     * @throws IllegalArgumentException se campos críticos forem inválidos
     */
    public LocalizacaoCargaEntity toEntity(final LocalizacaoCargaDTO dto) {
        if (dto == null) {
            return null;
        }

        // PROBLEMA #6: Validação de campos críticos
        final ResultadoValidacao validacao = ValidadorDTO.criarValidacao("LocalizacaoCarga");
        ValidadorDTO.validarId(validacao, "sequence_number", dto.getSequenceNumber());
        
        if (!validacao.isValido()) {
            validacao.logErros();
            throw new IllegalArgumentException("Localização de Carga inválida: sequence_number é obrigatório. Erros: " + validacao.getErros());
        }

        final LocalizacaoCargaEntity entity = new LocalizacaoCargaEntity();

        // 1. Mapeamento dos campos essenciais conforme docs/descobertas-endpoints/localizacaocarga.md
        entity.setSequenceNumber(dto.getSequenceNumber());
        entity.setType(dto.getType());
        entity.setInvoicesVolumes(dto.getInvoicesVolumes());
        entity.setTaxedWeight(dto.getTaxedWeight());
        entity.setInvoicesValue(dto.getInvoicesValue());
        entity.setServiceType(dto.getServiceType());
        entity.setBranchNickname(dto.getBranchNickname());
        entity.setDestinationLocationName(dto.getDestinationLocationName());
        entity.setDestinationBranchNickname(dto.getDestinationBranchNickname());
        entity.setClassification(dto.getClassification());
        entity.setStatus(dto.getStatus());
        entity.setStatusBranchNickname(dto.getStatusBranchNickname());
        entity.setFitFlnClnNickname(dto.getFitFlnClnNickname());
        entity.setOriginLocationName(dto.getOriginLocationName());
        entity.setOriginBranchNickname(dto.getOriginBranchNickname());

        // 2. Conversão segura de tipos de dados
        try {
            if (dto.getServiceAt() != null && !dto.getServiceAt().trim().isEmpty()) {
                entity.setServiceAt(OffsetDateTime.parse(dto.getServiceAt()));
            }
            if (dto.getPredictedDeliveryAt() != null && !dto.getPredictedDeliveryAt().trim().isEmpty()) {
                entity.setPredictedDeliveryAt(OffsetDateTime.parse(dto.getPredictedDeliveryAt()));
            }
            if (dto.getTotalValue() != null && !dto.getTotalValue().trim().isEmpty()) {
                entity.setTotalValue(new BigDecimal(dto.getTotalValue()));
            }
        } catch (DateTimeParseException | NumberFormatException e) {
            logger.error("❌ Erro ao converter dados para localização carga {}: serviceAt='{}', predictedDeliveryAt='{}', totalValue='{}' - {}", 
                dto.getSequenceNumber(), dto.getServiceAt(), dto.getPredictedDeliveryAt(), dto.getTotalValue(), e.getMessage());
            logger.debug("Stack trace completo:", e);
        }

        // 3. Empacotamento de todos os metadados
        String metadata = MapperUtil.toJson(dto.getAllProperties());
        entity.setMetadata(metadata);

        return entity;
    }
}
