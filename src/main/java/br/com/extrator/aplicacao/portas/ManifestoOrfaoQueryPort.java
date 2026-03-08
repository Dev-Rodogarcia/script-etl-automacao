package br.com.extrator.aplicacao.portas;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Porta de consulta para dados de manifestos orfaos.
 * Usada pelo pre-backfill referencial para determinar dinamicamente
 * a janela minima de retroatividade necessaria para cobrir coletas ausentes.
 */
public interface ManifestoOrfaoQueryPort {

    /**
     * Retorna a data mais antiga de criacao de um manifesto que possui
     * pick_sequence_code sem coleta correspondente no banco.
     * Retorna vazio se nao ha orphans registrados.
     */
    Optional<LocalDate> buscarDataMaisAntigaManifestoOrfao();
}
