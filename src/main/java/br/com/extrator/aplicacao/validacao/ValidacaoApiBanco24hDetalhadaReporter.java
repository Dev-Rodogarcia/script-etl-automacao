package br.com.extrator.aplicacao.validacao;

import static br.com.extrator.aplicacao.validacao.ValidacaoApiBanco24hDetalhadaTypes.ResumoExecucao;
import static br.com.extrator.aplicacao.validacao.ValidacaoApiBanco24hDetalhadaTypes.ResultadoComparacao;

import java.util.List;

import br.com.extrator.suporte.console.LoggerConsole;

final class ValidacaoApiBanco24hDetalhadaReporter {
    private final LoggerConsole log;
    private final ValidacaoApiBanco24hDetalhadaComparator comparator;

    ValidacaoApiBanco24hDetalhadaReporter(
        final LoggerConsole log,
        final ValidacaoApiBanco24hDetalhadaComparator comparator
    ) {
        this.log = log;
        this.comparator = comparator;
    }

    ResumoExecucao reportar(final List<ResultadoComparacao> resultados) {
        int totalOk = 0;
        int totalFalhas = 0;

        for (final ResultadoComparacao resultado : resultados) {
            final boolean divergenciaDinamicaTolerada = comparator.somenteDivergenciaDadosTolerada(resultado);
            final boolean inconclusivo =
                resultado.detalhe() != null && resultado.detalhe().startsWith("INCONCLUSIVO:");

            if (inconclusivo) {
                totalFalhas++;
                log.warn(
                    "API_VS_BANCO_24H_DETALHADO | entidade={} | status=INCONCLUSIVO | api_bruto={} | api_unico={} | invalidos={} | banco={} | faltantes={} | excedentes={} | divergencias_dados={}",
                    resultado.entidade(),
                    resultado.apiBruto(),
                    resultado.apiUnico(),
                    resultado.invalidos(),
                    resultado.banco(),
                    resultado.faltantes(),
                    resultado.excedentes(),
                    resultado.divergenciasDados()
                );
            } else if (resultado.ok()) {
                totalOk++;
                log.info(
                    "API_VS_BANCO_24H_DETALHADO | entidade={} | status=OK | api_bruto={} | api_unico={} | invalidos={} | banco={} | faltantes={} | excedentes={} | divergencias_dados={}",
                    resultado.entidade(),
                    resultado.apiBruto(),
                    resultado.apiUnico(),
                    resultado.invalidos(),
                    resultado.banco(),
                    resultado.faltantes(),
                    resultado.excedentes(),
                    resultado.divergenciasDados()
                );
            } else if (divergenciaDinamicaTolerada) {
                totalOk++;
                log.warn(
                    "API_VS_BANCO_24H_DETALHADO | entidade={} | status=OK_DADOS_DINAMICOS | api_bruto={} | api_unico={} | invalidos={} | banco={} | faltantes={} | excedentes={} | divergencias_dados={}",
                    resultado.entidade(),
                    resultado.apiBruto(),
                    resultado.apiUnico(),
                    resultado.invalidos(),
                    resultado.banco(),
                    resultado.faltantes(),
                    resultado.excedentes(),
                    resultado.divergenciasDados()
                );
            } else {
                totalFalhas++;
                log.error(
                    "API_VS_BANCO_24H_DETALHADO | entidade={} | status=FALHA | api_bruto={} | api_unico={} | invalidos={} | banco={} | faltantes={} | excedentes={} | divergencias_dados={}",
                    resultado.entidade(),
                    resultado.apiBruto(),
                    resultado.apiUnico(),
                    resultado.invalidos(),
                    resultado.banco(),
                    resultado.faltantes(),
                    resultado.excedentes(),
                    resultado.divergenciasDados()
                );
            }

            if (resultado.detalhe() != null && !resultado.detalhe().isBlank()) {
                log.info(
                    "API_VS_BANCO_24H_DETALHADO | entidade={} | detalhe={}",
                    resultado.entidade(),
                    resultado.detalhe()
                );
            }
        }

        log.console("=".repeat(88));
        log.info("RESUMO_API_VS_BANCO_24H_DETALHADO | ok={} | falhas={}", totalOk, totalFalhas);
        log.console("=".repeat(88));
        return new ResumoExecucao(totalOk, totalFalhas);
    }
}
