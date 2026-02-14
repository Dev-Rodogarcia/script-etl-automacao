package br.com.extrator.seguranca;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Utilitario de hash de senha com PBKDF2.
 */
public final class SenhaSeguraUtil {
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACOES = 120_000;
    private static final int TAMANHO_HASH_BITS = 256;
    private static final int TAMANHO_SALT_BYTES = 16;

    private SenhaSeguraUtil() {
    }

    public static String gerarSaltBase64() {
        final byte[] salt = new byte[TAMANHO_SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String gerarHashBase64(final char[] senha, final String saltBase64, final String pepper) {
        final byte[] salt = Base64.getDecoder().decode(saltBase64);
        final char[] senhaComPepper = combinarSenhaComPepper(senha, pepper);
        try {
            final PBEKeySpec spec = new PBEKeySpec(senhaComPepper, salt, ITERACOES, TAMANHO_HASH_BITS);
            final SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITMO);
            final byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (final NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Falha ao gerar hash de senha.", e);
        } finally {
            Arrays.fill(senhaComPepper, '\0');
        }
    }

    public static boolean validarSenha(
        final char[] senhaInformada,
        final String hashEsperadoBase64,
        final String saltBase64,
        final String pepper
    ) {
        final String hashCalculado = gerarHashBase64(senhaInformada, saltBase64, pepper);
        final byte[] esperado = Base64.getDecoder().decode(hashEsperadoBase64);
        final byte[] calculado = Base64.getDecoder().decode(hashCalculado);
        return comparacaoConstante(esperado, calculado);
    }

    private static boolean comparacaoConstante(final byte[] a, final byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int resultado = 0;
        for (int i = 0; i < a.length; i++) {
            resultado |= a[i] ^ b[i];
        }
        return resultado == 0;
    }

    private static char[] combinarSenhaComPepper(final char[] senha, final String pepper) {
        if (pepper == null || pepper.isEmpty()) {
            return Arrays.copyOf(senha, senha.length);
        }
        final String senhaStr = new String(senha);
        final String combinada = senhaStr + "::" + pepper;
        return combinada.toCharArray();
    }

    public static boolean senhaAtendePolitica(final char[] senha) {
        if (senha == null || senha.length < 8) {
            return false;
        }
        boolean temLetra = false;
        boolean temNumero = false;
        for (final char c : senha) {
            if (Character.isLetter(c)) {
                temLetra = true;
            } else if (Character.isDigit(c)) {
                temNumero = true;
            }
        }
        return temLetra && temNumero;
    }
}
