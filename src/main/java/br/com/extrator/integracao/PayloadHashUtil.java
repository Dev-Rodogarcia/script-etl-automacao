package br.com.extrator.integracao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class PayloadHashUtil {
    private PayloadHashUtil() {
    }

    static String sha256Hex(final String payload) {
        final String valorSeguro = payload == null ? "" : payload;
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(valorSeguro.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hash = new StringBuilder(digest.length * 2);
            for (final byte item : digest) {
                hash.append(String.format("%02x", item));
            }
            return hash.toString();
        } catch (final NoSuchAlgorithmException ex) {
            return "";
        }
    }
}
