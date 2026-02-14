package br.com.extrator.seguranca;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolve caminho do banco SQLite de seguranca.
 */
public final class CaminhoBancoSegurancaResolver {
    private static final Logger logger = LoggerFactory.getLogger(CaminhoBancoSegurancaResolver.class);
    private static final String ENV_DB_PATH = "EXTRATOR_SECURITY_DB_PATH";

    private CaminhoBancoSegurancaResolver() {
    }

    public static Path resolver() {
        final String caminhoCustomizado = System.getenv(ENV_DB_PATH);
        if (caminhoCustomizado != null && !caminhoCustomizado.trim().isEmpty()) {
            final Path custom = Paths.get(caminhoCustomizado.trim()).toAbsolutePath().normalize();
            criarDiretorioPai(custom);
            return custom;
        }

        final String programData = System.getenv("ProgramData");
        if (programData != null && !programData.trim().isEmpty()) {
            final Path prod = Paths.get(programData, "ExtratorESL", "security", "users.db").toAbsolutePath().normalize();
            try {
                criarDiretorioPai(prod);
                return prod;
            } catch (final RuntimeException e) {
                logger.warn("Nao foi possivel usar caminho em ProgramData ({}). Fallback para pasta local.", prod, e);
            }
        }

        final Path fallback = Paths.get("data", "security", "users.db").toAbsolutePath().normalize();
        criarDiretorioPai(fallback);
        return fallback;
    }

    private static void criarDiretorioPai(final Path arquivo) {
        try {
            final Path pai = arquivo.getParent();
            if (pai != null && !Files.exists(pai)) {
                Files.createDirectories(pai);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Falha ao criar diretorio do banco de seguranca: " + arquivo, e);
        }
    }
}
