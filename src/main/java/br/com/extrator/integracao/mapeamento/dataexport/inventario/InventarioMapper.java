package br.com.extrator.integracao.mapeamento.dataexport.inventario;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.dominio.dataexport.inventario.InventarioDTO;
import br.com.extrator.persistencia.entidade.InventarioEntity;
import br.com.extrator.suporte.formatacao.FormatadorData;
import br.com.extrator.suporte.mapeamento.MapperUtil;
import br.com.extrator.suporte.validacao.ValidadorDTO;
import br.com.extrator.suporte.validacao.ValidadorDTO.ResultadoValidacao;

public class InventarioMapper {

    private static final Logger logger = LoggerFactory.getLogger(InventarioMapper.class);

    public InventarioEntity toEntity(final InventarioDTO dto) {
        if (dto == null) {
            return null;
        }

        final ResultadoValidacao validacao = ValidadorDTO.criarValidacao("Inventario");
        ValidadorDTO.validarId(validacao, "sequence_code", dto.getSequenceCode());
        if (!validacao.isValido()) {
            validacao.logErros();
            throw new IllegalArgumentException("Inventario invalido: sequence_code e obrigatorio. Erros: " + validacao.getErros());
        }

        final InventarioEntity entity = new InventarioEntity();
        entity.setSequenceCode(dto.getSequenceCode());
        entity.setNumeroMinuta(dto.getNumeroMinuta());
        entity.setPagadorNome(dto.getPagadorNome());
        entity.setRemetenteNome(dto.getRemetenteNome());
        entity.setOrigemCidade(dto.getOrigemCidade());
        entity.setDestinatarioNome(dto.getDestinatarioNome());
        entity.setDestinoCidade(dto.getDestinoCidade());
        entity.setRegiaoEntrega(dto.getRegiaoEntrega());
        entity.setFilialEntregadora(dto.getFilialEntregadora());
        entity.setBranchNickname(dto.getBranchNickname());
        entity.setType(dto.getType());
        entity.setStatus(dto.getStatus());
        entity.setConferenteNome(dto.getConferenteNome());
        entity.setInvoicesMapping(dto.getInvoicesMapping() == null ? null : MapperUtil.toJson(dto.getInvoicesMapping()));
        entity.setInvoicesValue(converterParaBigDecimal(dto.getInvoicesValue(), "cnr_c_s_fit_invoices_value", dto.getSequenceCode()));
        entity.setRealWeight(converterParaBigDecimal(dto.getRealWeight(), "cnr_c_s_fit_real_weight", dto.getSequenceCode()));
        entity.setTotalCubicVolume(converterParaBigDecimal(dto.getTotalCubicVolume(), "cnr_c_s_fit_total_cubic_volume", dto.getSequenceCode()));
        entity.setTaxedWeight(converterParaBigDecimal(dto.getTaxedWeight(), "cnr_c_s_fit_taxed_weight", dto.getSequenceCode()));
        entity.setInvoicesVolumes(dto.getInvoicesVolumes());
        entity.setReadVolumes(dto.getReadVolumes());
        entity.setStartedAt(FormatadorData.parseOffsetDateTime(dto.getStartedAt()));
        entity.setFinishedAt(FormatadorData.parseOffsetDateTime(dto.getFinishedAt()));
        entity.setPredictedDeliveryAt(FormatadorData.parseOffsetDateTime(dto.getPredictedDeliveryAt()));
        entity.setPerformanceFinishedAt(FormatadorData.parseOffsetDateTime(dto.getPerformanceFinishedAt()));
        entity.setUltimaOcorrenciaAt(FormatadorData.parseOffsetDateTime(dto.getUltimaOcorrenciaAt()));
        entity.setUltimaOcorrenciaDescricao(dto.getUltimaOcorrenciaDescricao());

        final String metadata = MapperUtil.toJson(dto.getAllProperties());
        entity.setMetadata(metadata);
        entity.setIdentificadorUnico(calcularIdentificador(dto, entity.getInvoicesMapping(), metadata));
        return entity;
    }

    private BigDecimal converterParaBigDecimal(final String valor, final String campo, final Long sequenceCode) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(valor.trim());
        } catch (final NumberFormatException e) {
            logger.warn(
                "Falha ao converter {} no inventario {}: valor='{}'",
                campo,
                sequenceCode,
                valor
            );
            return null;
        }
    }

    private String calcularIdentificador(final InventarioDTO dto, final String invoicesMappingJson, final String metadata) {
        final String chaveCanonica = String.join(
            "|",
            String.valueOf(dto.getSequenceCode()),
            String.valueOf(dto.getNumeroMinuta()),
            invoicesMappingJson == null ? "" : invoicesMappingJson,
            dto.getStartedAt() == null ? "" : dto.getStartedAt()
        );
        return sha256Hex(chaveCanonica.isBlank() ? metadata : chaveCanonica);
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
            throw new IllegalStateException("Nao foi possivel calcular hash SHA-256 para inventario", e);
        }
    }
}
