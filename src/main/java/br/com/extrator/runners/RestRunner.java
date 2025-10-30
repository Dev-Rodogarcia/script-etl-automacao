package br.com.extrator.runners;

import java.time.LocalDate;

import br.com.extrator.servicos.ExtracaoServico;

/**
 * Runner independente para a API REST (Faturas e Ocorrências).
 * Agora utiliza o ExtracaoServico para garantir logging adequado
 * de todas as extrações na tabela log_extracoes.
 */
public final class RestRunner {

    private RestRunner() {}

    public static void executar(final LocalDate dataInicio) throws Exception {
        System.out.println("🔄 Executando runner REST...");

        // Valida conexão com banco
        br.com.extrator.util.CarregadorConfig.validarConexaoBancoDados();

        // Inicializa o serviço de extração
        final ExtracaoServico extracaoServico = new ExtracaoServico();

        // Executa todas as extrações usando o serviço
        System.out.println("\n📋 Executando extração completa com logging...");
        extracaoServico.executarExtracaoCompleta(dataInicio);
        
        System.out.println("✅ Extração REST finalizada com sucesso!");
    }
}