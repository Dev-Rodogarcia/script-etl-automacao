package br.com.extrator.integracao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

public final class DataExportReconciliationKeyExtractors {
    private static final Pattern DECIMAL_ZERO = Pattern.compile("-?\\d+\\.0");
    private static final String FPC_HASH_PREFIX = "FPC-HASH-";

    private DataExportReconciliationKeyExtractors() {
    }

    public static String manifesto(final JsonNode row) {
        final String sequence = cleanId(get(row, "sequence_code"));
        if (sequence == null) {
            return null;
        }
        final String pick = defaultIfNull(cleanId(get(row, "mft_pfs_pck_sequence_code")), "-1");
        final String mdfe = defaultIfNull(cleanId(get(row, "mft_mfs_number")), "-1");
        return sequence + "|" + pick + "|" + mdfe;
    }

    public static String faturaPorCliente(final JsonNode row) {
        final String nfse = cleanId(firstPresent(row, "fit_nse_number", "nfse_number"));
        final String cteNumber = cleanId(get(row, "fit_fhe_cte_number"));
        final String document = cleanId(get(row, "fit_ant_document"));
        final String billingId = cleanId(get(row, "billingId"));
        final StringBuilder canonical = new StringBuilder(512);

        if (nfse != null) {
            appendCampo(canonical, "identitySource", "nfse");
            appendCampo(canonical, "nfseNumber", nfse);
            appendCampo(canonical, "pagadorDocumento", rawValue(get(row, "fit_pyr_document")));
            appendCampo(canonical, "remetenteDocumento", rawValue(get(row, "fit_rpt_document")));
            appendCampo(canonical, "destinatarioDocumento", rawValue(get(row, "fit_sdr_document")));
            return FPC_HASH_PREFIX + sha256Hex(canonical.toString());
        }

        if (cteNumber != null) {
            appendCampo(canonical, "identitySource", "cte");
            appendCampo(canonical, "cteNumber", cteNumber);
            appendCampo(canonical, "pagadorDocumento", rawValue(get(row, "fit_pyr_document")));
            appendCampo(canonical, "remetenteDocumento", rawValue(get(row, "fit_rpt_document")));
            appendCampo(canonical, "destinatarioDocumento", rawValue(get(row, "fit_sdr_document")));
            return FPC_HASH_PREFIX + sha256Hex(canonical.toString());
        }

        if (document != null) {
            appendCampo(canonical, "identitySource", "fatura");
            appendCampo(canonical, "document", document);
            appendCampo(canonical, "issueDate", rawValue(get(row, "fit_ant_issue_date")));
            appendCampo(canonical, "pagadorDocumento", rawValue(get(row, "fit_pyr_document")));
            appendCampo(canonical, "destinatarioDocumento", rawValue(get(row, "fit_sdr_document")));
            return FPC_HASH_PREFIX + sha256Hex(canonical.toString());
        }

        if (billingId != null) {
            appendCampo(canonical, "identitySource", "billing");
            appendCampo(canonical, "billingId", billingId);
            appendCampo(canonical, "pagadorDocumento", rawValue(get(row, "fit_pyr_document")));
            appendCampo(canonical, "destinatarioDocumento", rawValue(get(row, "fit_sdr_document")));
            return FPC_HASH_PREFIX + sha256Hex(canonical.toString());
        }

        appendCampo(canonical, "identitySource", "fallback");
        appendCampo(canonical, "pagadorDocumento", rawValue(get(row, "fit_pyr_document")));
        appendCampo(canonical, "remetenteDocumento", rawValue(get(row, "fit_rpt_document")));
        appendCampo(canonical, "destinatarioDocumento", rawValue(get(row, "fit_sdr_document")));
        appendCampo(canonical, "notasFiscais", normalizarLista(get(row, "invoices_mapping")));
        appendCampo(canonical, "pedidosCliente", normalizarLista(get(row, "fit_fte_invoices_order_number")));
        appendCampo(canonical, "valorFrete", rawValue(get(row, "total")));
        appendCampo(canonical, "valorFatura", rawValue(get(row, "fit_ant_value")));
        return FPC_HASH_PREFIX + sha256Hex(canonical.toString());
    }

    static String cleanId(final JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        String text = rawValue(value);
        if (text == null) {
            return null;
        }
        text = text.trim();
        if (text.isEmpty()) {
            return null;
        }
        final String lower = text.toLowerCase(Locale.ROOT);
        if ("nan".equals(lower) || "none".equals(lower) || "null".equals(lower)) {
            return null;
        }
        if (DECIMAL_ZERO.matcher(text).matches()) {
            return text.substring(0, text.length() - 2);
        }
        return text;
    }

    static String normalizarLista(final JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return "<null>";
        }
        if (!value.isArray()) {
            return normalizarTexto(rawValue(value));
        }
        if (value.isEmpty()) {
            return "<null>";
        }

        final List<String> normalized = new ArrayList<>();
        for (final JsonNode item : value) {
            final String normalizado = normalizarTexto(rawValue(item));
            if (!"<null>".equals(normalizado) && !"<empty>".equals(normalizado)) {
                normalized.add(normalizado);
            }
        }
        if (normalized.isEmpty()) {
            return "<empty>";
        }
        Collections.sort(normalized);
        return String.join(",", normalized);
    }

    private static JsonNode get(final JsonNode row, final String fieldName) {
        if (row == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        return row.get(fieldName);
    }

    private static JsonNode firstPresent(final JsonNode row, final String... fieldNames) {
        if (row == null || fieldNames == null) {
            return null;
        }
        for (final String fieldName : fieldNames) {
            final JsonNode value = row.get(fieldName);
            if (value != null && !value.isNull() && !value.isMissingNode()) {
                return value;
            }
        }
        return null;
    }

    private static String rawValue(final JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        if (value.isTextual()) {
            return value.asText();
        }
        if (value.isNumber() || value.isBoolean()) {
            return value.asText();
        }
        return value.toString();
    }

    private static String normalizarTexto(final String value) {
        if (value == null) {
            return "<null>";
        }
        final String text = value.trim();
        return text.isEmpty() ? "<empty>" : text;
    }

    private static void appendCampo(final StringBuilder builder, final String name, final String value) {
        builder.append(name).append('=').append(normalizarTexto(value)).append('|');
    }

    private static String defaultIfNull(final String value, final String defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static String sha256Hex(final String value) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hex = new StringBuilder(hash.length * 2);
            for (final byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("Nao foi possivel calcular hash SHA-256 para chave de reconciliacao", e);
        }
    }
}
