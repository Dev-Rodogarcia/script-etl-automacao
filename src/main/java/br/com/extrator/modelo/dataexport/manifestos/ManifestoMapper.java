package br.com.extrator.modelo.dataexport.manifestos;

import br.com.extrator.db.entity.ManifestoEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Mapper (Tradutor) que transforma o ManifestoDTO (dados brutos do Data Export)
 * em uma ManifestoEntity (pronta para o banco de dados).
 * É responsável pela conversão de tipos e pela serialização de todos os
 * dados na coluna de metadados.
 */
public class ManifestoMapper {

    private static final Logger logger = LoggerFactory.getLogger(ManifestoMapper.class);

    private final ObjectMapper objectMapper;

    public ManifestoMapper() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Converte o DTO de Manifesto em uma Entidade.
     * @param dto O objeto DTO com os dados do manifesto.
     * @return Um objeto ManifestoEntity pronto para ser salvo.
     */
    public ManifestoEntity toEntity(ManifestoDTO dto) {
        if (dto == null) {
            return null;
        }

        ManifestoEntity entity = new ManifestoEntity();

        // 1. Mapeamento dos campos essenciais conforme docs/descobertas-endpoints/manifestos.md
        entity.setSequenceCode(dto.getSequenceCode());
        entity.setStatus(dto.getStatus());
        entity.setBranchNickname(dto.getBranchNickname());
        entity.setMdfeNumber(dto.getMdfeNumber());
        entity.setMdfeKey(dto.getMdfeKey());
        entity.setMdfeStatus(dto.getMdfeStatus());
        entity.setDistributionPole(dto.getDistributionPole());
        entity.setClassification(dto.getClassification());
        entity.setVehiclePlate(dto.getVehiclePlate());
        entity.setVehicleType(dto.getVehicleType());
        entity.setVehicleOwner(dto.getVehicleOwner());
        entity.setDriverName(dto.getDriverName());
        entity.setVehicleDepartureKm(dto.getVehicleDepartureKm());
        entity.setClosingKm(dto.getClosingKm());
        entity.setTraveledKm(dto.getTraveledKm());
        entity.setInvoicesCount(dto.getInvoicesCount());
        entity.setInvoicesVolumes(dto.getInvoicesVolumes());
        
        // Converter campos numéricos de String para BigDecimal
        entity.setInvoicesWeight(converterParaBigDecimal(dto.getInvoicesWeight(), "invoices_weight", dto.getSequenceCode()));
        entity.setTotalTaxedWeight(converterParaBigDecimal(dto.getTotalTaxedWeight(), "total_taxed_weight", dto.getSequenceCode()));
        entity.setTotalCubicVolume(converterParaBigDecimal(dto.getTotalCubicVolume(), "total_cubic_volume", dto.getSequenceCode()));
        entity.setInvoicesValue(converterParaBigDecimal(dto.getInvoicesValue(), "invoices_value", dto.getSequenceCode()));
        entity.setManifestFreightsTotal(converterParaBigDecimal(dto.getManifestFreightsTotal(), "manifest_freights_total", dto.getSequenceCode()));
        
        entity.setPickSequenceCode(dto.getPickSequenceCode());
        entity.setContractNumber(dto.getContractNumber());
        entity.setContractType(dto.getContractType());
        entity.setCalculationType(dto.getCalculationType());
        entity.setCargoType(dto.getCargoType());
        
        entity.setDailySubtotal(converterParaBigDecimal(dto.getDailySubtotal(), "daily_subtotal", dto.getSequenceCode()));
        entity.setOperationalExpensesTotal(converterParaBigDecimal(dto.getOperationalExpensesTotal(), "operational_expenses_total", dto.getSequenceCode()));
        entity.setInssValue(converterParaBigDecimal(dto.getInssValue(), "inss_value", dto.getSequenceCode()));
        entity.setSestSenatValue(converterParaBigDecimal(dto.getSestSenatValue(), "sest_senat_value", dto.getSequenceCode()));
        entity.setIrValue(converterParaBigDecimal(dto.getIrValue(), "ir_value", dto.getSequenceCode()));
        entity.setPayingTotal(converterParaBigDecimal(dto.getPayingTotal(), "paying_total", dto.getSequenceCode()));
        entity.setFreightSubtotal(converterParaBigDecimal(dto.getFreightSubtotal(), "freight_subtotal", dto.getSequenceCode()));
        entity.setFuelSubtotal(converterParaBigDecimal(dto.getFuelSubtotal(), "fuel_subtotal", dto.getSequenceCode()));
        entity.setTollSubtotal(converterParaBigDecimal(dto.getTollSubtotal(), "toll_subtotal", dto.getSequenceCode()));
        entity.setDriverServicesTotal(converterParaBigDecimal(dto.getDriverServicesTotal(), "driver_services_total", dto.getSequenceCode()));
        entity.setManualKm(dto.getManualKm());
        entity.setGenerateMdfe(dto.getGenerateMdfe());
        entity.setMonitoringRequest(dto.getMonitoringRequest());
        entity.setUniqDestinationsCount(dto.getUniqDestinationsCount());
        entity.setCreationUserName(dto.getCreationUserName());
        entity.setAdjustmentUserName(dto.getAdjustmentUserName());

        // 2. Conversão segura de tipos de dados
        try {
            if (dto.getCreatedAt() != null && !dto.getCreatedAt().trim().isEmpty()) {
                entity.setCreatedAt(OffsetDateTime.parse(dto.getCreatedAt()));
            }
            if (dto.getDeparturedAt() != null && !dto.getDeparturedAt().trim().isEmpty()) {
                entity.setDeparturedAt(OffsetDateTime.parse(dto.getDeparturedAt()));
            }
            if (dto.getClosedAt() != null && !dto.getClosedAt().trim().isEmpty()) {
                entity.setClosedAt(OffsetDateTime.parse(dto.getClosedAt()));
            }
            if (dto.getFinishedAt() != null && !dto.getFinishedAt().trim().isEmpty()) {
                entity.setFinishedAt(OffsetDateTime.parse(dto.getFinishedAt()));
            }
        } catch (DateTimeParseException e) {
            logger.error("❌ Erro ao converter datas para manifesto {}: createdAt='{}', departuredAt='{}', closedAt='{}', finishedAt='{}' - {}", 
                dto.getSequenceCode(), dto.getCreatedAt(), dto.getDeparturedAt(), dto.getClosedAt(), dto.getFinishedAt(), e.getMessage());
            logger.debug("Stack trace completo:", e);
        }
        
        // Converter totalCost usando o método auxiliar
        entity.setTotalCost(converterParaBigDecimal(dto.getTotalCost(), "total_cost", dto.getSequenceCode()));

        // 3. Empacotamento de todos os metadados
        try {
            String metadata = objectMapper.writeValueAsString(dto.getAllProperties());
            entity.setMetadata(metadata);
            
            // 4. Calcular identificador único APÓS definir metadata
            // Isso permite usar pick_sequence_code ou hash do metadata
            entity.calcularIdentificadorUnico();
            
        } catch (JsonProcessingException e) {
            logger.error("❌ CRÍTICO: Falha ao serializar metadados para manifesto {}: {}", 
                dto.getSequenceCode(), e.getMessage(), e);
            entity.setMetadata(String.format("{\"error\":\"Serialization failed\",\"sequence_code\":%d}", dto.getSequenceCode()));
            // Tentar calcular identificador mesmo com erro (usará fallback)
            entity.calcularIdentificadorUnico();
        }

        return entity;
    }
    
    /**
     * Converte uma String para BigDecimal de forma segura.
     * Retorna null se a string for null, vazia ou não puder ser convertida.
     * 
     * @param valor String a ser convertida
     * @param nomeCampo Nome do campo (para logs de erro)
     * @param sequenceCode Código sequencial do manifesto (para logs de erro)
     * @return BigDecimal ou null se não puder converter
     */
    private BigDecimal converterParaBigDecimal(String valor, String nomeCampo, Long sequenceCode) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException e) {
            logger.warn("⚠️ Erro ao converter campo '{}' para BigDecimal no manifesto {}: valor='{}'. Definindo como NULL.", 
                nomeCampo, sequenceCode, valor);
            return null;
        }
    }
}
