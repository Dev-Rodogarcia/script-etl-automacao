package br.com.extrator.modelo.dataexport.faturaporcliente;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.extrator.db.entity.FaturaPorClienteEntity;

/**
 * Mapper para converter FaturaPorClienteDTO em FaturaPorClienteEntity.
 * Implementa lógica de conversão, validação e cálculo de identificador único.
 * 
 * @author Sistema de Extração ESL Cloud
 * @version 1.0
 */
public class FaturaPorClienteMapper {
    private static final Logger logger = LoggerFactory.getLogger(FaturaPorClienteMapper.class);
    private final ObjectMapper objectMapper;
    private final DecimalFormat numberFormatUS;

    public FaturaPorClienteMapper() {
        this.objectMapper = new ObjectMapper();
        // CRÍTICO: Usar Locale.US para parsing correto (ponto decimal)
        this.numberFormatUS = (DecimalFormat) java.text.NumberFormat.getInstance(Locale.US);
        this.numberFormatUS.setParseBigDecimal(true);
    }

    /**
     * Converte DTO para Entity, aplicando todas as transformações necessárias.
     */
    public FaturaPorClienteEntity toEntity(final FaturaPorClienteDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DTO não pode ser null");
        }

        final FaturaPorClienteEntity entity = new FaturaPorClienteEntity();

        try {
            // 1. Calcular identificador único (PRIMEIRA PRIORIDADE)
            final String uniqueId = calcularIdentificadorUnico(dto);
            entity.setUniqueId(uniqueId);

            // 2. Documentos Fiscais (exclusividade NFS-e tem prioridade)
            final boolean isNfse = dto.getNfseNumber() != null;
            if (isNfse) {
                entity.setNumeroNfse(dto.getNfseNumber());
                entity.setNumeroCte(null);
                entity.setChaveCte(null);
                entity.setStatusCte(null);
                entity.setStatusCteResult(null);
                entity.setDataEmissaoCte(null);
            } else {
                final boolean isCte = (dto.getCteKey() != null && !dto.getCteKey().trim().isEmpty()) || dto.getCteNumber() != null;
                if (isCte) {
                    entity.setNumeroCte(dto.getCteNumber());
                    entity.setChaveCte(dto.getCteKey());
                    entity.setNumeroNfse(null);
                    entity.setStatusCte(traduzirStatus(dto.getCteStatus()));
                    entity.setStatusCteResult(dto.getCteStatusResult());
                    entity.setDataEmissaoCte(converterParaOffsetDateTime(dto.getCteIssuedAt()));
                } else {
                    entity.setNumeroCte(null);
                    entity.setChaveCte(null);
                    entity.setNumeroNfse(null);
                    entity.setStatusCte(null);
                    entity.setStatusCteResult(null);
                    entity.setDataEmissaoCte(null);
                }
            }

            // 3. Dados da Fatura
            entity.setNumeroFatura(dto.getFaturaDocument());
            entity.setDataEmissaoFatura(converterParaLocalDate(dto.getFaturaIssueDate()));
            entity.setDataVencimentoFatura(converterParaLocalDate(dto.getFaturaDueDate()));
            entity.setDataBaixaFatura(converterParaLocalDate(dto.getFaturaBaixaDate())); // Pode ser null
            entity.setFitAntOriginalDueDate(converterParaLocalDate(dto.getFaturaOriginalDueDate()));

            entity.setFitAntDocument(dto.getFaturaDocument());
            entity.setFitAntIssueDate(converterParaLocalDate(dto.getFaturaIssueDate()));
            entity.setFitAntValue(converterParaBigDecimal(dto.getFaturaValue()));

            // 4. Valores (USAR Locale.US)
            entity.setValorFrete(converterParaBigDecimal(dto.getValorFrete()));
            entity.setValorFatura(converterParaBigDecimal(dto.getFaturaValue()));
            entity.setThirdPartyCtesValue(converterParaBigDecimal(dto.getThirdPartyCtesValue()));

            // 5. Classificação Operacional
            entity.setFilial(dto.getFilial());
            entity.setTipoFrete(traduzirTipoFrete(dto.getTipoFrete()));
            entity.setClassificacao(dto.getClassificacao());
            entity.setEstado(dto.getEstado());

            // 6. Envolvidos
            entity.setPagadorNome(dto.getPagadorNome());
            entity.setPagadorDocumento(dto.getPagadorDocumento());
            entity.setRemetenteNome(dto.getRemetenteNome());
            entity.setRemetenteDocumento(dto.getRemetenteDocumento());
            entity.setDestinatarioNome(dto.getDestinatarioNome());
            entity.setDestinatarioDocumento(dto.getDestinatarioDocumento());
            entity.setVendedorNome(dto.getVendedorNome());

            // 7. Listas (converter arrays para strings)
            entity.setNotasFiscais(converterListaParaString(dto.getNotasFiscais()));
            entity.setPedidosCliente(converterListaParaString(dto.getPedidosCliente()));

            // 8. Metadata (JSON completo do DTO)
            final String metadata = objectMapper.writeValueAsString(dto);
            entity.setMetadata(metadata);

        } catch (final JsonProcessingException e) {
            logger.error("Erro ao mapear FaturaPorClienteDTO para Entity: {}", e.getMessage(), e);
            throw new RuntimeException("Falha no mapeamento de Fatura por Cliente", e);
        }

        return entity;
    }

    /**
     * Calcula o identificador único conforme regras de negócio:
     * 1. Se tiver chave de CT-e → usar chave (44 dígitos)
     * 2. Se não tiver CT-e mas tiver NFS-e → usar formato NFSE-{numero}
     * 3. Fallback: hash dos dados principais
     */
    private String calcularIdentificadorUnico(final FaturaPorClienteDTO dto) {
        if (dto.getNfseNumber() != null) {
            return "NFSE-" + dto.getNfseNumber();
        }
        if (dto.getCteKey() != null && !dto.getCteKey().trim().isEmpty()) {
            return dto.getCteKey().trim();
        }
        final String uuid = java.util.UUID.randomUUID().toString();
        logger.warn("Gerando unique_id via UUID por ausência de CT-e key e NFS-e number: {}", uuid);
        return uuid;
    }

    /**
     * Converte string para BigDecimal usando Locale.US (ponto decimal).
     * CRÍTICO: Usar Locale.US para evitar erro de parsing (123.69 != 12369.00).
     */
    private BigDecimal converterParaBigDecimal(final String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        try {
            // Remove espaços e usa Locale.US para parsing correto
            final String valorLimpo = valor.trim();
            return (BigDecimal) numberFormatUS.parse(valorLimpo);
        } catch (java.text.ParseException | ClassCastException e) {
            logger.warn("Erro ao converter valor '{}' para BigDecimal: {}", valor, e.getMessage());
            return null;
        }
    }

    /**
     * Converte string ISO para LocalDate (formato yyyy-MM-dd).
     */
    private LocalDate converterParaLocalDate(final String data) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(data.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (final DateTimeParseException e) {
            logger.warn("Erro ao converter data '{}' para LocalDate: {}", data, e.getMessage());
            return null;
        }
    }

    /**
     * Converte string ISO para OffsetDateTime.
     */
    private OffsetDateTime converterParaOffsetDateTime(final String dataHora) {
        if (dataHora == null || dataHora.trim().isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(dataHora.trim(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (final DateTimeParseException e) {
            logger.warn("Erro ao converter data/hora '{}' para OffsetDateTime: {}", dataHora, e.getMessage());
            return null;
        }
    }

    /**
     * Traduz status do CT-e (ex: "authorized" -> "Autorizado").
     */
    private String traduzirStatus(final String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        return switch (status.toLowerCase().trim()) {
            case "authorized" -> "Autorizado";
            case "cancelled" -> "Cancelado";
            case "denied" -> "Negado";
            case "pending" -> "Pendente";
            default -> status; // Retorna original se não houver tradução
        };
    }

    /**
     * Traduz tipo de frete (ex: "Freight::Normal" -> "Normal").
     */
    private String traduzirTipoFrete(final String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return null;
        }
        // Remove prefixo "Freight::" se presente
        final String tipoLimpo = tipo.replace("Freight::", "").trim();
        return tipoLimpo;
    }

    /**
     * Converte lista de strings para string única com separação por vírgula.
     * Exemplo: ["78427", "78428"] -> "78427, 78428"
     */
    private String converterListaParaString(final java.util.List<String> lista) {
        if (lista == null || lista.isEmpty()) {
            return null;
        }
        return String.join(", ", lista);
    }
}
