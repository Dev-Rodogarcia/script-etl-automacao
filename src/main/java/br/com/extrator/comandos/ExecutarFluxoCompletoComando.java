package br.com.extrator.comandos;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.auditoria.CompletudeValidator;
import br.com.extrator.runners.DataExportRunner;
import br.com.extrator.runners.GraphQLRunner;
import br.com.extrator.runners.RestRunner;
import br.com.extrator.util.BannerUtil;

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
        // Exibe banner inicial de extração completa
        BannerUtil.exibirBannerExtracaoCompleta();
        
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
            
            // ========== PASSO B: VALIDAÇÃO DE COMPLETUDE ==========
            // Somente após a conclusão bem-sucedida de todas as extrações,
            // instanciar e executar o CompletudeValidator
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🔍 INICIANDO VALIDAÇÃO DE COMPLETUDE DOS DADOS");
            System.out.println("=".repeat(60));
            
            try {
                // Instancia o validador
                final CompletudeValidator validator = new CompletudeValidator();
                
                // Passo B.1: Buscar totais da API ESL Cloud
                System.out.println("🔄 [1/2] Buscando totais nas APIs do ESL Cloud...");
                final LocalDate dataReferencia = LocalDate.now();
                final Optional<Map<String, Integer>> totaisEslCloudOpt = validator.buscarTotaisEslCloud(dataReferencia);
                
                if (totaisEslCloudOpt.isPresent()) {
                    final Map<String, Integer> totaisEslCloud = totaisEslCloudOpt.get();
                    System.out.println("✅ Totais obtidos das APIs com sucesso!");
                    
                    // Passo B.2: Validar completude comparando com o banco de dados
                    System.out.println("🔄 [2/2] Validando completude dos dados extraídos...");
                    final Map<String, CompletudeValidator.StatusValidacao> resultadosValidacao = validator.validarCompletude(totaisEslCloud, dataReferencia);
                    
                    // Determina se a extração está completa (todos os status devem ser OK)
                    boolean extracaoCompleta = resultadosValidacao.values().stream()
                        .allMatch(status -> status == CompletudeValidator.StatusValidacao.OK);
                    
                    // TÓPICO 4: Validações Avançadas (apenas se a validação básica passou)
                    boolean gapValidationOk = true;
                    boolean temporalValidationOk = true;
                    
                    if (extracaoCompleta) {
                        System.out.println("🔍 [3/4] Executando validação de gaps (IDs sequenciais)...");
                        CompletudeValidator.StatusValidacao gapStatus = validator.validarGapsOcorrencias(dataReferencia);
                        gapValidationOk = (gapStatus == CompletudeValidator.StatusValidacao.OK);
                        
                        if (gapValidationOk) {
                            System.out.println("✅ Validação de gaps: OK");
                            logger.info("✅ Validação de gaps concluída com sucesso");
                        } else {
                            System.out.println("⚠️ Validação de gaps: " + gapStatus);
                            logger.warn("⚠️ Validação de gaps detectou problemas: {}", gapStatus);
                        }
                        
                        System.out.println("🕐 [4/4] Executando validação de janela temporal...");
                        Map<String, CompletudeValidator.StatusValidacao> temporalResults = validator.validarJanelaTemporal(dataReferencia);
                        temporalValidationOk = temporalResults.values().stream()
                            .allMatch(status -> status == CompletudeValidator.StatusValidacao.OK);
                        
                        if (temporalValidationOk) {
                            System.out.println("✅ Validação temporal: OK");
                            logger.info("✅ Validação de janela temporal concluída com sucesso");
                        } else {
                            System.out.println("❌ Validação temporal detectou problemas críticos!");
                            logger.error("❌ Validação temporal detectou registros criados durante extração - risco de perda de dados");
                        }
                    }
                    
                    // Determina resultado final considerando todas as validações
                    boolean validacaoFinalCompleta = extracaoCompleta && gapValidationOk && temporalValidationOk;
                    
                    // Exibe resultado final da validação
                    System.out.println("\n" + "=".repeat(60));
                    if (validacaoFinalCompleta) {
                        System.out.println("🎉 EXTRAÇÃO 100% COMPLETA E VALIDADA!");
                        System.out.println("✅ Todos os dados foram extraídos com sucesso!");
                        System.out.println("✅ Validação de gaps: OK");
                        System.out.println("✅ Validação temporal: OK");
                        logger.info("🎉 EXTRAÇÃO 100% COMPLETA! Todas as validações (básica, gaps e temporal) foram bem-sucedidas.");
                    } else {
                        System.out.println("❌ EXTRAÇÃO COM PROBLEMAS - Verificar logs");
                        if (!extracaoCompleta) {
                            System.out.println("⚠️  Inconsistências na contagem de registros detectadas.");
                        }
                        if (!gapValidationOk) {
                            System.out.println("⚠️  Gaps nos IDs detectados - possível perda de registros específicos.");
                        }
                        if (!temporalValidationOk) {
                            System.out.println("❌ CRÍTICO: Registros criados durante extração - risco de perda de dados!");
                        }
                        System.out.println("💡 Consulte os logs detalhados para identificar os problemas.");
                        logger.error("❌ EXTRAÇÃO COM PROBLEMAS - Básica: {}, Gaps: {}, Temporal: {}", 
                            extracaoCompleta, gapValidationOk, temporalValidationOk);
                        
                        // Nota: Implementação futura de alertas por email/Slack pode ser adicionada aqui
                    }
                    System.out.println("=".repeat(60));
                } else {
                    System.out.println("ℹ️ Continuando sem validação de completude (API indisponível)");
                    logger.info("ℹ️ Continuando sem validação de completude (API indisponível)");
                }
                
            } catch (Exception e) {
                logger.warn("⚠️ Não foi possível comparar com API ESL Cloud: {}", e.getMessage());
                logger.info("ℹ️ Continuando sem validação de completude (dados extraídos estão salvos)");
                logger.debug("Stack trace completo da falha na validação:", e);
                System.out.println("⚠️ Validação de completude falhou - continuando sem validação");
                System.out.println("ℹ️ Os dados extraídos foram salvos com sucesso no banco de dados");
            }
            
            // Exibe resumo final
            final LocalDateTime fimExecucao = LocalDateTime.now();
            
            // Exibe banner de sucesso
            BannerUtil.exibirBannerSucesso();
            
            System.out.println("📊 RESUMO DA EXTRAÇÃO");
            System.out.println("Início: " + inicioExecucao.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            System.out.println("Fim: " + fimExecucao.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            System.out.println("Duração: " + java.time.Duration.between(inicioExecucao, fimExecucao).toMinutes() + " minutos");
            System.out.println("✅ Todas as 3 APIs foram processadas e validadas!");
            System.out.println();
            
            // Grava timestamp de execução bem-sucedida
            gravarDataExecucao();
            
        } catch (final Exception e) {
            // Exibe banner de erro
            BannerUtil.exibirBannerErro();
            System.err.println("❌ Erro durante execução: " + e.getMessage());
            logger.error("Falha durante a execução do fluxo completo: {}", e.getMessage(), e);
            throw e; // Re-propaga para tratamento de alto nível
        }
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