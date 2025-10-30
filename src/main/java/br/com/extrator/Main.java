package br.com.extrator;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.comandos.Comando;
import br.com.extrator.comandos.ExibirAjudaComando;
import br.com.extrator.comandos.ExecutarAuditoriaComando;
import br.com.extrator.comandos.ExecutarFluxoCompletoComando;
import br.com.extrator.comandos.LimparTabelasComando;
import br.com.extrator.comandos.RealizarIntrospeccaoGraphQLComando;
import br.com.extrator.comandos.TestarApiComando;
import br.com.extrator.comandos.ValidarAcessoComando;
import br.com.extrator.comandos.VerificarTimestampsComando;
import br.com.extrator.comandos.VerificarTimezoneComando;
import br.com.extrator.servicos.LoggingService;

/**
 * Sistema de Extração de Dados do ESL Cloud - Orquestrador Principal
 *
 * Esta classe atua como um orquestrador de alto nível que delega a execução
 * do processo de ETL para as classes Runner especializadas:
 * - RestRunner: Faturas a Receber, Faturas a Pagar e Ocorrências
 * - GraphQLRunner: Coletas e Fretes
 * - DataExportRunner: Manifestos, Cotações e Localização de Carga
 *
 * Responsabilidades da Main:
 * - Interpretar argumentos da linha de comando
 * - Delegar execução para os Runners apropriados
 * - Manter métodos utilitários (banner, ajuda, logging)
 * - Tratamento de erros de alto nível
 *
 * @author Sistema de Extração ESL Cloud
 * @version 3.0 - Refatorado para padrão Orquestrador
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    // Mapa de comandos disponíveis
    private static final Map<String, Comando> COMANDOS = Map.of(
        "--fluxo-completo", new ExecutarFluxoCompletoComando(),
        "--validar", new ValidarAcessoComando(),
        "--ajuda", new ExibirAjudaComando(),
        "--help", new ExibirAjudaComando(),
        "--introspeccao", new RealizarIntrospeccaoGraphQLComando(),
        "--auditoria", new ExecutarAuditoriaComando(),
        "--testar-api", new TestarApiComando(),
        "--limpar-tabelas", new LimparTabelasComando(),
        "--verificar-timestamps", new VerificarTimestampsComando(),
        "--verificar-timezone", new VerificarTimezoneComando()
    );

    public static void main(final String[] args) {
        // Inicializa o sistema de logging para capturar saída do terminal
        final LoggingService loggingService = new LoggingService();
        loggingService.iniciarCaptura("extracao_dados");
        
        // Organiza quaisquer logs .txt gerados na raiz, movendo-os para a pasta logs/
        organizarLogsTxtNaPastaLogs();
        
        try {
            // Organiza logs .txt na pasta logs
            organizarLogsTxtNaPastaLogs();
            
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