package br.com.extrator.modelo.dataexport.faturaporcliente;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.db.entity.FaturaPorClienteEntity;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

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
    public FaturaPorClienteEntity toEntity(FaturaPorClienteDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DTO não pode ser null");
        }

        FaturaPorClienteEntity entity = new FaturaPorClienteEntity();

        try {
            // 1. Calcular identificador único (PRIMEIRA PRIORIDADE)
            String uniqueId = calcularIdentificadorUnico(dto);
            entity.setUniqueId(uniqueId);

            // 2. Documentos Fiscais
            entity.setNumeroCte(dto.getCteNumber());
            entity.setChaveCte(dto.getCteKey());
            entity.setNumeroNfse(dto.getNfseNumber());
            entity.setStatusCte(traduzirStatus(dto.getCteStatus()));
            entity.setDataEmissaoCte(converterParaOffsetDateTime(dto.getCteIssuedAt()));

            // 3. Dados da Fatura
            entity.setNumeroFatura(dto.getFaturaDocument());
            entity.setDataEmissaoFatura(converterParaLocalDate(dto.getFaturaIssueDate()));
            entity.setDataVencimentoFatura(converterParaLocalDate(dto.getFaturaDueDate()));
            entity.setDataBaixaFatura(converterParaLocalDate(dto.getFaturaBaixaDate())); // Pode ser null

            entity.setFitAntDocument(dto.getFaturaDocument());
            entity.setFitAntIssueDate(converterParaLocalDate(dto.getFaturaIssueDate()));
            entity.setFitAntValue(converterParaBigDecimal(dto.getFaturaValue()));

            // 4. Valores (USAR Locale.US)
            entity.setValorFrete(converterParaBigDecimal(dto.getValorFrete()));
            entity.setValorFatura(converterParaBigDecimal(dto.getFaturaValue()));

            // 5. Classificação Operacional
            entity.setFilial(dto.getFilial());
            entity.setTipoFrete(traduzirTipoFrete(dto.getTipoFrete()));
            entity.setClassificacao(dto.getClassificacao());
            entity.setEstado(dto.getEstado());

            // 6. Envolvidos
            entity.setPagadorNome(dto.getPagadorNome());
            entity.setPagadorDocumento(dto.getPagadorDocumento());
            entity.setRemetenteNome(dto.getRemetenteNome());
            entity.setDestinatarioNome(dto.getDestinatarioNome());
            entity.setVendedorNome(dto.getVendedorNome());

            // 7. Listas (converter arrays para strings)
            entity.setNotasFiscais(converterListaParaString(dto.getNotasFiscais()));
            entity.setPedidosCliente(converterListaParaString(dto.getPedidosCliente()));

            // 8. Metadata (JSON completo do DTO)
            String metadata = objectMapper.writeValueAsString(dto);
            entity.setMetadata(metadata);

        } catch (JsonProcessingException e) {
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
    private String calcularIdentificadorUnico(FaturaPorClienteDTO dto) {
        // Prioridade 1: Chave de CT-e
        if (dto.getCteKey() != null && !dto.getCteKey().trim().isEmpty()) {
            return dto.getCteKey().trim();
        }

        // Prioridade 2: Número de NFS-e
        if (dto.getNfseNumber() != null) {
            return "NFSE-" + dto.getNfseNumber();
        }

        // Prioridade 3: Hash dos dados principais (fallback)
        String dadosCombinados = String.format("%s|%s|%s|%s",
                dto.getFaturaDocument() != null ? dto.getFaturaDocument() : "",
                dto.getPagadorDocumento() != null ? dto.getPagadorDocumento() : "",
                dto.getFaturaIssueDate() != null ? dto.getFaturaIssueDate() : "",
                dto.getValorFrete() != null ? dto.getValorFrete() : "");

        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(dadosCombinados.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "HASH-" + hexString.toString().substring(0, 32); // Primeiros 32 chars
        } catch (java.security.NoSuchAlgorithmException e) {
            logger.error("Erro ao gerar hash para identificador único: {}", e.getMessage());
            // Fallback final: usar timestamp
            return "UNKNOWN-" + System.currentTimeMillis();
        }
    }

    /**
     * Converte string para BigDecimal usando Locale.US (ponto decimal).
     * CRÍTICO: Usar Locale.US para evitar erro de parsing (123.69 != 12369.00).
     */
    private BigDecimal converterParaBigDecimal(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        try {
            // Remove espaços e usa Locale.US para parsing correto
            String valorLimpo = valor.trim();
            return (BigDecimal) numberFormatUS.parse(valorLimpo);
        } catch (java.text.ParseException | ClassCastException e) {
            logger.warn("Erro ao converter valor '{}' para BigDecimal: {}", valor, e.getMessage());
            return null;
        }
    }

    /**
     * Converte string ISO para LocalDate (formato yyyy-MM-dd).
     */
    private LocalDate converterParaLocalDate(String data) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(data.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            logger.warn("Erro ao converter data '{}' para LocalDate: {}", data, e.getMessage());
            return null;
        }
    }

    /**
     * Converte string ISO para OffsetDateTime.
     */
    private OffsetDateTime converterParaOffsetDateTime(String dataHora) {
        if (dataHora == null || dataHora.trim().isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(dataHora.trim(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException e) {
            logger.warn("Erro ao converter data/hora '{}' para OffsetDateTime: {}", dataHora, e.getMessage());
            return null;
        }
    }

    /**
     * Traduz status do CT-e (ex: "authorized" -> "Autorizado").
     */
    private String traduzirStatus(String status) {
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
    private String traduzirTipoFrete(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return null;
        }
        // Remove prefixo "Freight::" se presente
        String tipoLimpo = tipo.replace("Freight::", "").trim();
        return tipoLimpo;
    }

    /**
     * Converte lista de strings para string única com separação por vírgula.
     * Exemplo: ["78427", "78428"] -> "78427, 78428"
     */
    private String converterListaParaString(java.util.List<String> lista) {
        if (lista == null || lista.isEmpty()) {
            return null;
        }
        return String.join(", ", lista);
    }
}
