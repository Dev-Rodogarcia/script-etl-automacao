package br.com.extrator.comandos;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.runners.DataExportRunner;
import br.com.extrator.runners.GraphQLRunner;
import br.com.extrator.runners.RestRunner;

/**
 * Comando responsável por executar o fluxo completo de extração de dados
 * das 3 APIs do ESL Cloud (REST, GraphQL e DataExport).
 */
public class ExecutarFluxoCompletoComando implements Comando {
    private static final Logger logger = LoggerFactory.getLogger(ExecutarFluxoCompletoComando.class);
    
    // Constantes para gravação do timestamp de execução
    private static final String ARQUIVO_ULTIMO_RUN = "last_run.properties";
    private static final String PROPRIEDADE_ULTIMO_RUN = "last_successful_run";
    
    @Override
    public void executar(String[] args) throws Exception {
        // Exibe banner inicial
        exibirBanner();
        
        // Define data de hoje para buscar dados do dia atual
        final LocalDate dataHoje = LocalDate.now();
        
        logger.info("Iniciando processo de extração de dados das 3 APIs do ESL Cloud");
        System.out.println("\n" + "=".repeat(60));
        System.out.println("INICIANDO PROCESSO DE EXTRAÇÃO DE DADOS");
        System.out.println("=".repeat(60));
        System.out.println("Modo: DADOS DE HOJE");
        System.out.println("Data de extração: " + dataHoje.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " (dados de hoje)");
        System.out.println("Início: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        System.out.println("=".repeat(60) + "\n");
        
        final LocalDateTime inicioExecucao = LocalDateTime.now();
        
        try {
            // Executa API REST (Faturas e Ocorrências)
            System.out.println("🔄 [1/3] Executando API REST...");
            RestRunner.executar(dataHoje);
            System.out.println("✅ API REST concluída com sucesso!");
            
            // Pausa entre APIs para respeitar rate limit global
            System.out.println("⏳ Aguardando 2 segundos antes da próxima API...");
            Thread.sleep(2000);
            
            // Executa API GraphQL (Coletas e Fretes)
            System.out.println("\n🔄 [2/3] Executando API GraphQL...");
            GraphQLRunner.executar(dataHoje);
            System.out.println("✅ API GraphQL concluída com sucesso!");
            
            // Pausa entre APIs para respeitar rate limit global
            System.out.println("⏳ Aguardando 2 segundos antes da próxima API...");
            Thread.sleep(2000);
            
            // Executa API Data Export (Manifestos, Cotações, Localização)
            System.out.println("\n🔄 [3/3] Executando API Data Export...");
            DataExportRunner.executar(dataHoje);
            System.out.println("✅ API Data Export concluída com sucesso!");
            
            // Exibe resumo final
            final LocalDateTime fimExecucao = LocalDateTime.now();
            System.out.println("\n" + "=".repeat(60));
            System.out.println("PROCESSO DE EXTRAÇÃO CONCLUÍDO COM SUCESSO");
            System.out.println("=".repeat(60));
            System.out.println("Início: " + inicioExecucao.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            System.out.println("Fim: " + fimExecucao.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            System.out.println("Duração: " + java.time.Duration.between(inicioExecucao, fimExecucao).toMinutes() + " minutos");
            System.out.println("✅ Todas as 3 APIs foram processadas com sucesso!");
            System.out.println("=".repeat(60));
            
            // Grava timestamp de execução bem-sucedida
            gravarDataExecucao();
            
        } catch (final Exception e) {
            logger.error("Falha durante a execução do fluxo completo: {}", e.getMessage(), e);
            throw e; // Re-propaga para tratamento de alto nível
        }
    }
    
    private void exibirBanner() {
        System.out.println("=".repeat(80));
        System.out.println("🚀 SISTEMA DE EXTRAÇÃO DE DADOS - ESL CLOUD");
        System.out.println("=".repeat(80));
        System.out.println("Versão: 3.0 - Arquitetura Refatorada com Padrão Command");
        System.out.println("Desenvolvido para automação de extração de dados via APIs");
        System.out.println("=".repeat(80));
    }
    
    /**
     * Grava timestamp da execução bem-sucedida.
     */
    private void gravarDataExecucao() {
        try {
            final Properties props = new Properties();
            props.setProperty(PROPRIEDADE_ULTIMO_RUN, LocalDateTime.now().toString());
            
            try (final FileOutputStream fos = new FileOutputStream(ARQUIVO_ULTIMO_RUN)) {
                props.store(fos, "Última execução bem-sucedida do sistema de extração");
            }
            
            logger.info("Timestamp de execução gravado com sucesso");
        } catch (final IOException e) {
            logger.warn("Não foi possível gravar timestamp de execução: {}", e.getMessage());
        }
    }
}