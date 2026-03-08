package br.com.extrator.aplicacao.validacao;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

final class ValidacaoApiBanco24hDetalhadaTypes {
    private ValidacaoApiBanco24hDetalhadaTypes() {
    }

    @FunctionalInterface
    interface ResultadoApiChavesSupplier {
        ResultadoApiChaves get() throws Exception;
    }

    record EntidadeValidacao(
        String entidade,
        ResultadoApiChavesSupplier fornecedor
    ) { }

    record JanelaExecucao(
        LocalDateTime inicio,
        LocalDateTime fim,
        boolean alinhadaAoPeriodo
    ) { }

    record ResultadoApiChaves(
        int apiBruto,
        int apiUnico,
        int invalidos,
        Set<String> chaves,
        Map<String, String> hashesPorChave,
        Map<String, Set<String>> hashesAceitosPorChave,
        String detalhe,
        Set<String> chavesToleradasNoBanco
    ) { }

    record ResultadoComparacao(
        String entidade,
        int apiBruto,
        int apiUnico,
        int invalidos,
        int banco,
        int faltantes,
        int excedentes,
        int divergenciasDados,
        String detalhe
    ) {
        boolean ok() {
            return faltantes == 0 && excedentes == 0 && divergenciasDados == 0;
        }
    }

    record ResumoExecucao(int ok, int falhas) { }
}
