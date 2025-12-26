package br.com.extrator;

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
import br.com.extrator.comandos.validacao.ValidarManifestosComando;
import br.com.extrator.comandos.validacao.VerificarTimestampsComando;
import br.com.extrator.comandos.validacao.VerificarTimezoneComando;
import br.com.extrator.servicos.LoggingService;

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
 * @author Sistema de Extração ESL Cloud
 * @version 3.1 - Arquitetura com 2 Runners (GraphQL + DataExport)
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    // Mapa de comandos disponíveis
    private static final Map<String, Comando> COMANDOS = criarMapaComandos();
    
    private static Map<String, Comando> criarMapaComandos() {
        Map<String, Comando> comandos = new HashMap<>();
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
        
        try {
            
            // Determina o comando a ser executado
            final String nomeComando = (args.length == 0) ? "--fluxo-completo" : args[0].toLowerCase();
            
            // Busca e executa o comando
            final Comando comando = COMANDOS.getOrDefault(nomeComando, new ExibirAjudaComando());
            
            if (!COMANDOS.containsKey(nomeComando)) {
                System.err.println("❌ Comando desconhecido: " + nomeComando);
                System.err.println("💡 Use --ajuda para ver os comandos disponíveis.");
            }
            
            comando.executar(args);
            
        } catch (final Exception e) {
            logger.error("Erro durante execução: {}", e.getMessage(), e);
            System.err.println("❌ Erro durante execução: " + e.getMessage());
            // Salva os logs antes de encerrar com código de erro
            loggingService.pararCaptura();
            System.exit(1);
        } finally {
            // Finaliza captura de logs
            loggingService.pararCaptura();
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
