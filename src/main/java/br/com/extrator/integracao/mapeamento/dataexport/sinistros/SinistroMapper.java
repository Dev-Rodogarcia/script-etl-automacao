package br.com.extrator.integracao.mapeamento.dataexport.sinistros;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.dominio.dataexport.sinistros.SinistroDTO;
import br.com.extrator.persistencia.entidade.SinistroEntity;
import br.com.extrator.suporte.formatacao.FormatadorData;
import br.com.extrator.suporte.mapeamento.HorarioUtil;
import br.com.extrator.suporte.mapeamento.MapperUtil;
import br.com.extrator.suporte.validacao.ValidadorDTO;
import br.com.extrator.suporte.validacao.ValidadorDTO.ResultadoValidacao;

public class SinistroMapper {

    private static final Logger logger = LoggerFactory.getLogger(SinistroMapper.class);

    public SinistroEntity toEntity(final SinistroDTO dto) {
        if (dto == null) {
            return null;
        }

        final ResultadoValidacao validacao = ValidadorDTO.criarValidacao("Sinistro");
        ValidadorDTO.validarId(validacao, "sequence_code", dto.getSequenceCode());
        if (!validacao.isValido()) {
            validacao.logErros();
            throw new IllegalArgumentException("Sinistro invalido: sequence_code e obrigatorio. Erros: " + validacao.getErros());
        }

        final SinistroEntity entity = new SinistroEntity();
        entity.setSequenceCode(dto.getSequenceCode());
        entity.setOpeningAtDate(FormatadorData.parseLocalDate(dto.getOpeningAtDate()));
        entity.setOccurrenceAtDate(FormatadorData.parseLocalDate(dto.getOccurrenceAtDate()));
        entity.setOccurrenceAtTime(HorarioUtil.normalizarHora(dto.getOccurrenceAtTime()));
        entity.setExpectedSolutionDate(FormatadorData.parseLocalDate(dto.getExpectedSolutionDate()));
        entity.setInsuranceClaimLocation(dto.getInsuranceClaimLocation());
        entity.setInformedBy(dto.getInformedBy());
        entity.setFinishedAtDate(FormatadorData.parseLocalDate(dto.getFinishedAtDate()));
        entity.setFinishedAtTime(HorarioUtil.normalizarHora(dto.getFinishedAtTime()));
        entity.setInvoicesCount(dto.getInvoicesCount());
        entity.setCorporationSequenceNumber(dto.getCorporationSequenceNumber());
        entity.setInsuranceOccurrenceNumber(dto.getInsuranceOccurrenceNumber());
        entity.setInvoicesVolumes(dto.getInvoicesVolumes());
        entity.setInvoicesWeight(converterParaBigDecimal(dto.getInvoicesWeight(), "invoices_weight", dto.getSequenceCode()));
        entity.setInvoicesValue(converterParaBigDecimal(dto.getInvoicesValue(), "invoices_value", dto.getSequenceCode()));
        entity.setPayerNickname(dto.getPayerNickname());
        entity.setCustomerDebitsSubtotal(converterParaBigDecimal(dto.getCustomerDebitsSubtotal(), "customer_debits_subtotal", dto.getSequenceCode()));
        entity.setCustomerCreditEntriesSubtotal(converterParaBigDecimal(dto.getCustomerCreditEntriesSubtotal(), "customer_credit_entries_subtotal", dto.getSequenceCode()));
        entity.setResponsibleCreditsSubtotal(converterParaBigDecimal(dto.getResponsibleCreditsSubtotal(), "responsible_credits_subtotal", dto.getSequenceCode()));
        entity.setResponsibleDebitEntriesSubtotal(converterParaBigDecimal(dto.getResponsibleDebitEntriesSubtotal(), "responsible_debit_entries_subtotal", dto.getSequenceCode()));
        entity.setInsurerCreditsSubtotal(converterParaBigDecimal(dto.getInsurerCreditsSubtotal(), "insurer_credits_subtotal", dto.getSequenceCode()));
        entity.setInsuranceClaimTotal(converterParaBigDecimal(dto.getInsuranceClaimTotal(), "insurance_claim_total", dto.getSequenceCode()));
        entity.setBranchNickname(dto.getBranchNickname());
        entity.setEventName(dto.getEventName());
        entity.setUserName(dto.getUserName());
        entity.setVehiclePlate(dto.getVehiclePlate());
        entity.setOccurrenceDescription(dto.getOccurrenceDescription());
        entity.setOccurrenceCode(dto.getOccurrenceCode());
        entity.setTreatmentAt(FormatadorData.parseOffsetDateTime(dto.getTreatmentAt()));
        entity.setDealingType(dto.getDealingType());
        entity.setSolutionType(dto.getSolutionType());
        final String metadata = MapperUtil.toJson(dto.getAllProperties());
        entity.setMetadata(metadata);
        entity.setIdentificadorUnico(calcularIdentificador(dto, metadata));
        return entity;
    }

    private BigDecimal converterParaBigDecimal(final String valor, final String campo, final Long sequenceCode) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(valor.trim());
        } catch (final NumberFormatException e) {
            logger.warn("Falha ao converter {} no sinistro {}: valor='{}'", campo, sequenceCode, valor);
            return null;
        }
    }

    private String calcularIdentificador(final SinistroDTO dto, final String metadata) {
        final String chaveCanonica = String.join(
            "|",
            String.valueOf(dto.getSequenceCode()),
            String.valueOf(dto.getInsuranceOccurrenceNumber()),
            String.valueOf(dto.getCorporationSequenceNumber())
        );
        return sha256Hex(chaveCanonica.equals("null|null|null") ? metadata : chaveCanonica);
    }

    private String sha256Hex(final String valor) {
        try {
            final byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(valor.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hex = new StringBuilder(hash.length * 2);
            for (final byte item : hash) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("Nao foi possivel calcular hash SHA-256 para sinistros", e);
        }
    }
}
