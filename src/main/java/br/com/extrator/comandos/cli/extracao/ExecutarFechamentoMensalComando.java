package br.com.extrator.comandos.cli.extracao;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Supplier;

import br.com.extrator.aplicacao.extracao.ExtracaoPorIntervaloRequest;
import br.com.extrator.aplicacao.extracao.ExtracaoPorIntervaloUseCase;
import br.com.extrator.comandos.cli.base.Comando;
import br.com.extrator.suporte.console.LoggerConsole;
import br.com.extrator.suporte.formatacao.FormatadorData;
import br.com.extrator.suporte.tempo.RelogioSistema;

public class ExecutarFechamentoMensalComando implements Comando {
    private static final LoggerConsole log = LoggerConsole.getLogger(ExecutarFechamentoMensalComando.class);

    private final ExtracaoPorIntervaloUseCase extracaoPorIntervaloUseCase;
    private final Supplier<LocalDate> hojeSupplier;

    public ExecutarFechamentoMensalComando() {
        this(new ExtracaoPorIntervaloUseCase(), RelogioSistema::hoje);
    }

    ExecutarFechamentoMensalComando(final ExtracaoPorIntervaloUseCase extracaoPorIntervaloUseCase,
                                    final Supplier<LocalDate> hojeSupplier) {
        this.extracaoPorIntervaloUseCase = extracaoPorIntervaloUseCase;
        this.hojeSupplier = hojeSupplier;
    }

    @Override
    public void executar(final String[] args) throws Exception {
        final LocalDate hoje = hojeSupplier.get();
        final YearMonth mesAnterior = YearMonth.from(hoje).minusMonths(1);
        final LocalDate dataInicio = mesAnterior.atDay(1);
        final LocalDate dataFim = mesAnterior.atEndOfMonth();

        log.console(
            "Executando fechamento mensal do mes anterior: {} a {}",
            FormatadorData.formatBR(dataInicio),
            FormatadorData.formatBR(dataFim)
        );

        extracaoPorIntervaloUseCase.executar(new ExtracaoPorIntervaloRequest(
            dataInicio,
            dataFim,
            null,
            null,
            false,
            false,
            ExtracaoPorIntervaloRequest.ModoExecucao.INTERVALO
        ));
    }
}
