package br.com.extrator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.comandos.console.ExibirAjudaComando;
import br.com.extrator.comandos.auditoria.ExecutarAuditoriaComando;
import br.com.extrator.comandos.base.Comando;
import br.com.extrator.comandos.extracao.ExecutarFluxoCompletoComando;
import br.com.extrator.comandos.utilitarios.ExportarCsvComando;
import br.com.extrator.comandos.utilitarios.LimparTabelasComando;
import br.com.extrator.comandos.utilitarios.RealizarIntrospeccaoGraphQLComando;
import br.com.extrator.comandos.utilitarios.TestarApiComando;
import br.com.extrator.comandos.validacao.ValidarAcessoComando;
import br.com.extrator.comandos.validacao.ValidarDadosCompletoComando;
import br.com.extrator.comandos.validacao.ValidarManifestosComando;
import br.com.extrator.comandos.validacao.VerificarTimestampsComando;
import br.com.extrator.comandos.validacao.VerificarTimezoneComando;
import br.com.extrator.servicos.LoggingService;
import br.com.extrator.auditoria.execucao.ExecutionAuditor;
import br.com.extrator.db.repository.ExecutionHistoryRepository;

/**
 * Sistema de Extração de Dados do ESL Cloud - Orquestrador Principal
 *
 * Esta classe atua como um orquestrador de alto nível que delega a execução
 * do processo de ETL para as classes Runner especializadas:
 * - GraphQLRunner: Coletas, Fretes e Faturas (via GraphQL)
 * - DataExportRunner: Manifestos, Cotações, Localização de Carga, 
 *                     Contas a Pagar e Faturas por Cliente (via Data Export API)
 *
 * Responsabilidades da Main:
 * - Interpretar argumentos da linha de comando
 * - Delegar execução para os Runners apropriados
 * - Manter métodos utilitários (banner, ajuda, logging)
 * - Tratamento de erros de alto nível
 *
 * @author Lucas Andrade (@valentelucass) - lucasmac.dev@gmail.com
 * @version 3.1 - Arquitetura com 2 Runners (GraphQL + DataExport)
 * 
 * 💡 Este sistema foi desenvolvido com atenção aos detalhes e seguindo as melhores práticas
 *    de engenharia de dados. Cada linha de código reflete o cuidado e a dedicação do desenvolvedor.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    // Mapa de comandos disponíveis
    private static final Map<String, Comando> COMANDOS = criarMapaComandos();
    
    private static Map<String, Comando> criarMapaComandos() {
        final Map<String, Comando> comandos = new HashMap<>();
        comandos.put("--fluxo-completo", new ExecutarFluxoCompletoComando());
        comandos.put("--extracao-intervalo", new br.com.extrator.comandos.extracao.ExecutarExtracaoPorIntervaloComando());
        comandos.put("--loop", new br.com.extrator.comandos.extracao.LoopExtracaoComando());
        comandos.put("--validar", new ValidarAcessoComando());
        comandos.put("--ajuda", new ExibirAjudaComando());
        comandos.put("--help", new ExibirAjudaComando());
        comandos.put("--introspeccao", new RealizarIntrospeccaoGraphQLComando());
        comandos.put("--auditoria", new ExecutarAuditoriaComando());
        comandos.put("--testar-api", new TestarApiComando());
        comandos.put("--limpar-tabelas", new LimparTabelasComando());
        comandos.put("--verificar-timestamps", new VerificarTimestampsComando());
        comandos.put("--verificar-timezone", new VerificarTimezoneComando());
        comandos.put("--validar-manifestos", new ValidarManifestosComando());
        comandos.put("--validar-dados", new ValidarDadosCompletoComando());
        comandos.put("--exportar-csv", new ExportarCsvComando());
        return comandos;
    }

    public static void main(final String[] args) {
        // Inicializa o sistema de logging para capturar saída do terminal
        final LoggingService loggingService = new LoggingService();
        loggingService.iniciarCaptura("extracao_dados");
        // Garante que os logs sejam salvos mesmo em encerramentos abruptos
        Runtime.getRuntime().addShutdownHook(new Thread(() -> loggingService.pararCaptura()));
        
        // Organiza quaisquer logs .txt gerados na raiz, movendo-os para a pasta logs/
        organizarLogsTxtNaPastaLogs();
        
        final LocalDateTime inicioExecucao = LocalDateTime.now();
        String status = "SUCCESS";
        String errorMessage = null;
        String errorCategory = null;
        int exitCode = 0;
        
        // Determina o comando a ser executado
        final String nomeComando = (args.length == 0) ? "--fluxo-completo" : args[0].toLowerCase();
        final String tipoExecucao = nomeComando.startsWith("--") ? nomeComando.substring(2) : nomeComando;
        
        try {
            
            // Busca e executa o comando
            final Comando comando = COMANDOS.getOrDefault(nomeComando, new ExibirAjudaComando());
            
            if (!COMANDOS.containsKey(nomeComando)) {
                System.err.println("❌ Comando desconhecido: " + nomeComando);
                System.err.println("💡 Use --ajuda para ver os comandos disponíveis.");
            }
            
            comando.executar(args);
            
        } catch (final Exception e) {
            status = "ERROR";
            errorMessage = e.getMessage();
            errorCategory = e.getClass().getSimpleName();
            exitCode = 1;
            logger.error("Erro durante execução: {}", e.getMessage(), e);
            System.err.println("❌ Erro durante execução: " + e.getMessage());
            
            
        } finally {
            final LocalDateTime fimExecucao = LocalDateTime.now();
            final long duracaoSegundos = Duration.between(inicioExecucao, fimExecucao).getSeconds();
            final int duracaoSegundosInt = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, duracaoSegundos));
            
            int totalRecords = 0;
            try {
                final ExecutionHistoryRepository repo = new ExecutionHistoryRepository();
                totalRecords = repo.calcularTotalRegistros(inicioExecucao, fimExecucao);
            } catch (final Throwable t) {
                logger.warn("Falha ao calcular total de registros: {}", t.getMessage());
            }
            
            ExecutionAuditor.registrarCsv(
                fimExecucao,
                status,
                duracaoSegundosInt,
                totalRecords,
                tipoExecucao,
                errorMessage
            );
            
            try {
                final ExecutionHistoryRepository repo = new ExecutionHistoryRepository();
                repo.inserirHistorico(
                    inicioExecucao,
                    fimExecucao,
                    duracaoSegundosInt,
                    status,
                    totalRecords,
                    errorCategory,
                    errorMessage
                );
            } catch (final Throwable t) {
                logger.warn("Falha ao gravar historico de execucao no banco: {}", t.getMessage());
            }
            
            // Finaliza captura de logs
            loggingService.pararCaptura();
        }
        
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    // ========== MÉTODOS UTILITÁRIOS ==========

    /**
     * Organiza arquivos de log .txt na pasta logs.
     */
    private static void organizarLogsTxtNaPastaLogs() {
        LoggingService.organizarLogsTxtNaPastaLogs();
    }
}
