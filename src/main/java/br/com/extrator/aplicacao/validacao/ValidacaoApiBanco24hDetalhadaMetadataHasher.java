/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/aplicacao/validacao/ValidacaoApiBanco24hDetalhadaMetadataHasher.java
Classe  : ValidacaoApiBanco24hDetalhadaMetadataHasher (final class)
Pacote  : br.com.extrator.aplicacao.validacao
Modulo  : Use Case - Validacao

Papel   : Gerador de hashes SHA256 para metadados de entidades (com remocao de campos volateis por entidade).

Conecta com:
- MapperUtil (suporte.mapeamento)
- ConstantesEntidades (suporte.validacao)
- LoggerConsole (suporte.console)

Fluxo geral:
1) hashMetadata(entidade, metadata) normaliza JSON removendo campos volateis.
2) Calcula SHA256 em formato hex da string normalizada.
3) Com fallback: JSON parsing error -> usa string trim().

Estrutura interna:
Atributos-chave:
- log: LoggerConsole statico para debug.
Metodos principais:
- hashMetadata(String, String): retorna hash SHA256 hex.
- normalizarMetadataParaComparacao(): parse JSON, remove campos volateis por switch de entidade.
- removerCamposVolateisComparacao(): remove status, datas de predicao, valores, volumes (dependendo entidade).
[DOC-FILE-END]============================================================== */
package br.com.extrator.aplicacao.validacao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import br.com.extrator.suporte.console.LoggerConsole;
import br.com.extrator.suporte.mapeamento.MapperUtil;
import br.com.extrator.suporte.validacao.ConstantesEntidades;

final class ValidacaoApiBanco24hDetalhadaMetadataHasher {
    private static final LoggerConsole log =
        LoggerConsole.getLogger(ValidacaoApiBanco24hDetalhadaMetadataHasher.class);

    String hashMetadata(final String entidade, final String metadata) {
        final String normalizado = normalizarMetadataParaComparacao(entidade, metadata);
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(normalizado.getBytes(StandardCharsets.UTF_8));
            final StringBuilder sb = new StringBuilder(hash.length * 2);
            for (final byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nao disponivel", e);
        }
    }

    private String normalizarMetadataParaComparacao(final String entidade, final String metadata) {
        if (metadata == null || metadata.trim().isEmpty()) {
            return "__NULL__";
        }

        try {
            final JsonNode parsed = MapperUtil.sharedJson().readTree(metadata);
            if (!parsed.isObject()) {
                return metadata.trim();
            }
            final ObjectNode obj = (ObjectNode) parsed.deepCopy();
            removerCamposVolateisComparacao(entidade, obj);
            return MapperUtil.sharedJson().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.debug(
                "Fallback de normalizacao de metadata por erro de parse JSON | entidade={} | erro={}",
                entidade,
                e.getOriginalMessage()
            );
            return metadata.trim();
        }
    }

    private void removerCamposVolateisComparacao(final String entidade, final ObjectNode obj) {
        if (entidade == null || obj == null) {
            return;
        }
        switch (entidade) {
            case ConstantesEntidades.LOCALIZACAO_CARGAS -> obj.remove("fit_fln_status");
            case ConstantesEntidades.FRETES -> {
                obj.remove("status");
                obj.remove("deliveryPredictionDate");
                obj.remove("delivery_prediction_date");
            }
            case ConstantesEntidades.COLETAS -> {
                obj.remove("invoicesValue");
                obj.remove("invoicesVolumes");
                obj.remove("invoicesWeight");
                obj.remove("taxedWeight");
            }
            default -> {
                // Sem tratamento especial para outras entidades.
            }
        }
    }
}
