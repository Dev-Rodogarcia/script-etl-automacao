package br.com.extrator.suporte.json;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import br.com.extrator.suporte.mapeamento.MapperUtil;

/**
 * Gera representacao canonica de JSON com ordenacao profunda de chaves
 * para permitir hashing deterministico de payloads semanticamente equivalentes.
 */
public final class CanonicalJsonHasher {
    private CanonicalJsonHasher() {
    }

    public static String canonicalize(final String json) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (json == null || json.trim().isEmpty()) {
            return "__NULL__";
        }
        final JsonNode node = MapperUtil.sharedJson().readTree(json);
        return canonicalize(node);
    }

    public static String canonicalize(final JsonNode node) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (node == null || node.isNull()) {
            return "__NULL__";
        }
        return MapperUtil.sharedJson().writeValueAsString(sortDeep(node));
    }

    public static String sha256Hex(final String value) {
        final String normalized = value == null ? "__NULL__" : value;
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            final StringBuilder sb = new StringBuilder(hash.length * 2);
            for (final byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nao disponivel", e);
        }
    }

    public static String sha256CanonicalJson(final String json) throws com.fasterxml.jackson.core.JsonProcessingException {
        return sha256Hex(canonicalize(json));
    }

    public static String sha256CanonicalJson(final JsonNode node) throws com.fasterxml.jackson.core.JsonProcessingException {
        return sha256Hex(canonicalize(node));
    }

    private static JsonNode sortDeep(final JsonNode node) {
        if (node == null || node.isNull()) {
            return NullNode.instance;
        }
        if (node.isObject()) {
            final ObjectNode objectNode = MapperUtil.sharedJson().createObjectNode();
            final Map<String, JsonNode> sorted = new TreeMap<>();
            final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> field = fields.next();
                sorted.put(field.getKey(), sortDeep(field.getValue()));
            }
            sorted.forEach(objectNode::set);
            return objectNode;
        }
        if (node.isArray()) {
            final ArrayNode arrayNode = MapperUtil.sharedJson().createArrayNode();
            for (final JsonNode item : node) {
                arrayNode.add(sortDeep(item));
            }
            return arrayNode;
        }
        return node.deepCopy();
    }
}
