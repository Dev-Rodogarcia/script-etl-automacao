package br.com.extrator.comandos.extracao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import br.com.extrator.comandos.base.Comando;
import br.com.extrator.runners.dataexport.DataExportRunner;
import br.com.extrator.runners.graphql.GraphQLRunner;
import br.com.extrator.servicos.ValidadorLimiteExtracao;
import br.com.extrator.util.console.BannerUtil;
import br.com.extrator.util.console.LoggerConsole;
import br.com.extrator.util.formatacao.FormatadorData;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Comando responsável por executar extração de dados por intervalo de datas,
 * com divisão automática em blocos de 30 dias e validação de regras de limitação.
 */
public class ExecutarExtracaoPorIntervaloComando implements Comando {
    
    private static final LoggerConsole log = LoggerConsole.getLogger(ExecutarExtracaoPorIntervaloComando.class);
    
    private static final int TAMANHO_BLOCO_DIAS = 30;
    private static final int NUMERO_DE_THREADS = 2;
    
    @Override
    public void executar(final String[] args) throws Exception {
        // Validar argumentos
        if (args.length < 3) {
            log.error("❌ ERRO: Argumentos insuficientes");
            log.console("Uso: --extracao-intervalo YYYY-MM-DD YYYY-MM-DD [api] [entidade]");
            log.console("Exemplo: --extracao-intervalo 2024-11-01 2025-03-31");
            log.console("Exemplo: --extracao-intervalo 2024-11-01 2025-03-31 graphql");
            log.console("Exemplo: --extracao-intervalo 2024-11-01 2025-03-31 dataexport manifestos");
            return;
        }
        
        // Parse das datas
        final LocalDate dataInicio;
        final LocalDate dataFim;
        try {
            dataInicio = LocalDate.parse(args[1], DateTimeFormatter.ISO_DATE);
            dataFim = LocalDate.parse(args[2], DateTimeFormatter.ISO_DATE);
        } catch (final DateTimeParseException e) {
            log.error("❌ ERRO: Formato de data inválido. Use YYYY-MM-DD");
            log.console("Exemplo: 2024-11-01 2025-03-31");
            return;
        }
        
        // Parse de API e entidade (opcionais)
        String apiEspecifica = null;
        String entidadeEspecifica = null;
        if (args.length >= 4) {
            final String arg3 = args[3].trim().toLowerCase();
            // Validar se o terceiro argumento é uma API válida
            if ("graphql".equals(arg3) || "dataexport".equals(arg3)) {
                apiEspecifica = arg3;
                if (args.length >= 5) {
                    entidadeEspecifica = args[4].trim();
                }
            } else {
                // Se não for uma API válida, pode ser que a entidade foi passada sem a API
                // Nesse caso, tentamos inferir a API baseado na entidade
                log.warn("⚠️ Terceiro argumento '{}' não é uma API válida. Tentando inferir API pela entidade...", arg3);
                entidadeEspecifica = args[3].trim();
                
                // Tentar inferir a API baseado na entidade
                final String entidadeLower = entidadeEspecifica.toLowerCase();
                if (entidadeLower.equals(ConstantesEntidades.COLETAS) || 
                    entidadeLower.equals(ConstantesEntidades.FRETES) || 
                    entidadeLower.equals(ConstantesEntidades.FATURAS_GRAPHQL)) {
                    apiEspecifica = "graphql";
                    log.info("✅ API inferida: GraphQL (baseado na entidade: {})", entidadeEspecifica);
                } else if (entidadeLower.equals(ConstantesEntidades.MANIFESTOS) ||
                          entidadeLower.equals(ConstantesEntidades.COTACOES) ||
                          entidadeLower.equals(ConstantesEntidades.LOCALIZACAO_CARGAS) ||
                          entidadeLower.equals(ConstantesEntidades.CONTAS_A_PAGAR) ||
                          entidadeLower.equals(ConstantesEntidades.FATURAS_POR_CLIENTE)) {
                    apiEspecifica = "dataexport";
                    log.info("✅ API inferida: DataExport (baseado na entidade: {})", entidadeEspecifica);
                } else {
                    log.error("❌ Não foi possível inferir a API para a entidade: {}. Use: --extracao-intervalo DATA_INICIO DATA_FIM [api] [entidade]", entidadeEspecifica);
                    return;
                }
            }
        }
        
        // Validar que dataInicio <= dataFim
        if (dataInicio.isAfter(dataFim)) {
            log.error("❌ ERRO: Data de início ({}) não pode ser posterior à data de fim ({})", 
                     FormatadorData.formatBR(dataInicio), FormatadorData.formatBR(dataFim));
            return;
        }
        
        // Exibir banner
        BannerUtil.exibirBannerExtracaoCompleta();
        
        log.console("\n" + "=".repeat(60));
        log.console("EXTRAÇÃO POR INTERVALO DE DATAS");
        log.console("=".repeat(60));
        log.console("Período solicitado: {} a {}", 
                   FormatadorData.formatBR(dataInicio), FormatadorData.formatBR(dataFim));
        
        // Exibir filtros se especificados
        if (apiEspecifica != null && !apiEspecifica.isEmpty()) {
            log.console("API: {}", apiEspecifica.toUpperCase());
            if (entidadeEspecifica != null && !entidadeEspecifica.isEmpty()) {
                log.console("Entidade: {}", entidadeEspecifica);
            } else {
                log.console("Entidade: TODAS");
            }
        } else {
            log.console("API: TODAS");
            log.console("Entidade: TODAS");
        }
        
        // Calcular duração do período
        final ValidadorLimiteExtracao validador = new ValidadorLimiteExtracao();
        final long diasPeriodo = validador.calcularDuracaoPeriodo(dataInicio, dataFim);
        log.console("Duração: {} dias", diasPeriodo);
        
        // Obter limite de horas baseado no período TOTAL (apenas informativo)
        // NOTA: A validação real será feita por BLOCO (30 dias), não pelo período total.
        // Cada bloco de 30 dias será tratado como "< 31 dias" (sem limite de horas).
        final int limiteHorasPeriodoTotal = validador.obterLimiteHoras(diasPeriodo);
        if (limiteHorasPeriodoTotal == 0) {
            log.console("Regra de limitação: SEM LIMITE (período < 31 dias)");
        } else if (limiteHorasPeriodoTotal == 1) {
            log.console("Regra de limitação: 1 HORA entre extrações (período de 31 dias a 6 meses)");
        } else {
            log.console("Regra de limitação: 12 HORAS entre extrações (período > 6 meses)");
        }
        log.console("ℹ️  Nota: Validação será feita por BLOCO (30 dias = sem limite), não pelo período total");
        
        // Dividir em blocos de 30 dias se necessário
        final List<BlocoPeriodo> blocos = dividirEmBlocos(dataInicio, dataFim);
        log.console("Total de blocos: {}", blocos.size());
        
        if (blocos.size() > 1) {
            log.console("\n📦 Período será dividido em {} blocos de até {} dias:", blocos.size(), TAMANHO_BLOCO_DIAS);
            for (int i = 0; i < blocos.size(); i++) {
                final BlocoPeriodo bloco = blocos.get(i);
                log.console("  Bloco {}/{}: {} a {} ({} dias)", 
                           i + 1, blocos.size(),
                           FormatadorData.formatBR(bloco.dataInicio),
                           FormatadorData.formatBR(bloco.dataFim),
                           validador.calcularDuracaoPeriodo(bloco.dataInicio, bloco.dataFim));
            }
        }
        
        log.console("=".repeat(60) + "\n");
        
        // Executar extração para cada bloco
        final LocalDateTime inicioExecucao = LocalDateTime.now();
        int blocosCompletos = 0;
        int blocosFalhados = 0;
        final List<String> blocosFalhadosLista = new ArrayList<>();
        
        for (int i = 0; i < blocos.size(); i++) {
            final BlocoPeriodo bloco = blocos.get(i);
            final int numeroBloco = i + 1;
            final int totalBlocos = blocos.size();
            
            log.console("\n" + "=".repeat(60));
            log.console("BLOCO {}/{}: {} a {}", 
                       numeroBloco, totalBlocos,
                       FormatadorData.formatBR(bloco.dataInicio),
                       FormatadorData.formatBR(bloco.dataFim));
            log.console("=".repeat(60));
            
            // CORRIGIDO: Validar regras de limitação usando o TAMANHO DO BLOCO (30 dias), não período total
            // Estratégia: Dividir em blocos de 30 dias para evitar a regra de 12 horas.
            // Cada bloco de 30 dias será tratado como "< 31 dias" (sem limite de horas).
            final boolean podeExecutar = validarLimitesParaBloco(bloco, validador, apiEspecifica, entidadeEspecifica);
            
            if (!podeExecutar) {
                log.warn("⏸️ Bloco {}/{} bloqueado pelas regras de limitação. Pulando...", numeroBloco, totalBlocos);
                blocosFalhados++;
                blocosFalhadosLista.add("Bloco " + numeroBloco);
                continue;
            }
            
            // Executar extração do bloco
            try {
                log.info("🔄 Iniciando extração do bloco {}/{}...", numeroBloco, totalBlocos);
                
                // Se API específica foi informada, executar apenas essa API
                if (apiEspecifica != null && !apiEspecifica.isEmpty()) {
                    if ("graphql".equals(apiEspecifica)) {
                        GraphQLRunner.executarPorIntervalo(bloco.dataInicio, bloco.dataFim, entidadeEspecifica);
                    } else if ("dataexport".equals(apiEspecifica)) {
                        DataExportRunner.executarPorIntervalo(bloco.dataInicio, bloco.dataFim, entidadeEspecifica);
                    } else {
                        log.error("❌ API inválida: {}. Use 'graphql' ou 'dataexport'", apiEspecifica);
                        blocosFalhados++;
                        blocosFalhadosLista.add("Bloco " + numeroBloco);
                        continue;
                    }
                } else {
                    // Executar ambas as APIs em paralelo
                    final ExecutorService executor = Executors.newFixedThreadPool(NUMERO_DE_THREADS);
                    try {
                        final Future<?> futuroGraphQL = executor.submit(() -> {
                            try {
                                GraphQLRunner.executarPorIntervalo(bloco.dataInicio, bloco.dataFim);
                            } catch (final Exception e) {
                                throw new RuntimeException("Falha no GraphQLRunner", e);
                            }
                        });
                        
                        final Future<?> futuroDataExport = executor.submit(() -> {
                            try {
                                DataExportRunner.executarPorIntervalo(bloco.dataInicio, bloco.dataFim);
                            } catch (final Exception e) {
                                throw new RuntimeException("Falha no DataExportRunner", e);
                            }
                        });
                        
                        // Aguardar conclusão
                        futuroGraphQL.get();
                        futuroDataExport.get();
                    } finally {
                        executor.shutdown();
                    }
                }
                
                log.info("✅ Bloco {}/{} (entidades principais) concluído com sucesso!", numeroBloco, totalBlocos);
                
                // ========== FASE 3: EXTRAÇÃO DE FATURAS GRAPHQL POR ÚLTIMO ==========
                // Executar faturas_graphql APÓS todas as outras entidades do bloco,
                // EXCETO se a entidade específica for faturas_graphql (já foi executada acima)
                final boolean isSomenteFaturasGraphQL = entidadeEspecifica != null && 
                    (ConstantesEntidades.FATURAS_GRAPHQL.equalsIgnoreCase(entidadeEspecifica) ||
                     "faturas".equalsIgnoreCase(entidadeEspecifica) ||
                     "faturasgraphql".equalsIgnoreCase(entidadeEspecifica));
                
                final boolean deveExecutarFaturasGraphQL = 
                    (apiEspecifica == null || "graphql".equalsIgnoreCase(apiEspecifica)) &&
                    !isSomenteFaturasGraphQL;
                
                if (deveExecutarFaturasGraphQL) {
                    log.info("🔄 [FASE 3] Executando Faturas GraphQL por último para bloco {}/{}...", numeroBloco, totalBlocos);
                    try {
                        GraphQLRunner.executarFaturasGraphQLPorIntervalo(bloco.dataInicio, bloco.dataFim);
                        log.info("✅ Faturas GraphQL do bloco {}/{} concluídas!", numeroBloco, totalBlocos);
                    } catch (final Exception e) {
                        log.error("❌ Falha na extração de Faturas GraphQL do bloco {}/{}: {}. Dados das outras entidades foram preservados.", 
                                 numeroBloco, totalBlocos, e.getMessage(), e);
                        // Não incrementamos blocosFalhados aqui pois as outras entidades foram extraídas com sucesso
                    }
                }
                
                blocosCompletos++;
                
            } catch (final Exception e) {
                log.error("❌ Falha na extração do bloco {}/{}: {}", numeroBloco, totalBlocos, e.getMessage(), e);
                blocosFalhados++;
                blocosFalhadosLista.add("Bloco " + numeroBloco);
            }
        }
        
        // Exibir resumo final
        final LocalDateTime fimExecucao = LocalDateTime.now();
        final long duracaoMinutos = java.time.Duration.between(inicioExecucao, fimExecucao).toMinutes();
        
        log.console("\n" + "=".repeat(60));
        log.console("RESUMO DA EXTRAÇÃO POR INTERVALO");
        log.console("=".repeat(60));
        log.console("Período: {} a {}", 
                   FormatadorData.formatBR(dataInicio), FormatadorData.formatBR(dataFim));
        log.console("Total de blocos: {}", blocos.size());
        log.console("Blocos completos: {}", blocosCompletos);
        if (blocosFalhados > 0) {
            log.warn("Blocos falhados: {} - {}", blocosFalhados, String.join(", ", blocosFalhadosLista));
        }
        log.console("Duração total: {} minutos", duracaoMinutos);
        log.console("=".repeat(60));
        
        if (blocosFalhados == 0) {
            BannerUtil.exibirBannerSucesso();
        } else {
            BannerUtil.exibirBannerErro();
        }
    }
    
    /**
     * Divide o período em blocos de 30 dias.
     * 
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return Lista de blocos de período
     */
    private List<BlocoPeriodo> dividirEmBlocos(final LocalDate dataInicio, final LocalDate dataFim) {
        final List<BlocoPeriodo> blocos = new ArrayList<>();
        
        LocalDate inicioBloco = dataInicio;
        
        while (!inicioBloco.isAfter(dataFim)) {
            // Calcular fim do bloco (início + 30 dias, ou dataFim se for menor)
            final LocalDate fimBloco = inicioBloco.plusDays(TAMANHO_BLOCO_DIAS - 1);
            final LocalDate fimReal = fimBloco.isAfter(dataFim) ? dataFim : fimBloco;
            
            blocos.add(new BlocoPeriodo(inicioBloco, fimReal));
            
            // Próximo bloco começa no dia seguinte ao fim do bloco atual
            inicioBloco = fimReal.plusDays(1);
        }
        
        return blocos;
    }
    
    /**
     * Valida regras de limitação para as entidades do bloco.
     * CORRIGIDO: A regra de limitação é baseada no TAMANHO DO BLOCO (30 dias), não no período total.
     * Isso permite que blocos de 30 dias usem a regra de "sem limite" em vez de 12 horas.
     * 
     * @param bloco Bloco de período a validar
     * @param validador Validador de limites
     * @param apiEspecifica API específica (null = todas)
     * @param entidadeEspecifica Entidade específica (null = todas)
     * @return true se pode executar, false se bloqueado
     */
    private boolean validarLimitesParaBloco(final BlocoPeriodo bloco, 
                                           final ValidadorLimiteExtracao validador,
                                           final String apiEspecifica,
                                           final String entidadeEspecifica) {
        // Determinar quais entidades validar
        final List<String> entidadesParaValidar = new ArrayList<>();
        
        if (entidadeEspecifica != null && !entidadeEspecifica.isEmpty()) {
            // Validar apenas a entidade específica
            entidadesParaValidar.add(entidadeEspecifica);
        } else if (apiEspecifica != null && !apiEspecifica.isEmpty()) {
            // Validar todas as entidades da API específica
            if ("graphql".equals(apiEspecifica)) {
                entidadesParaValidar.add(ConstantesEntidades.COLETAS);
                entidadesParaValidar.add(ConstantesEntidades.FRETES);
                entidadesParaValidar.add(ConstantesEntidades.FATURAS_GRAPHQL);
            } else if ("dataexport".equals(apiEspecifica)) {
                entidadesParaValidar.add(ConstantesEntidades.MANIFESTOS);
                entidadesParaValidar.add(ConstantesEntidades.COTACOES);
                entidadesParaValidar.add(ConstantesEntidades.LOCALIZACAO_CARGAS);
                entidadesParaValidar.add(ConstantesEntidades.CONTAS_A_PAGAR);
                entidadesParaValidar.add(ConstantesEntidades.FATURAS_POR_CLIENTE);
            }
        } else {
            // Validar todas as entidades
            entidadesParaValidar.add(ConstantesEntidades.COLETAS);
            entidadesParaValidar.add(ConstantesEntidades.FRETES);
            entidadesParaValidar.add(ConstantesEntidades.MANIFESTOS);
            entidadesParaValidar.add(ConstantesEntidades.COTACOES);
            entidadesParaValidar.add(ConstantesEntidades.LOCALIZACAO_CARGAS);
            entidadesParaValidar.add(ConstantesEntidades.CONTAS_A_PAGAR);
            entidadesParaValidar.add(ConstantesEntidades.FATURAS_POR_CLIENTE);
            entidadesParaValidar.add(ConstantesEntidades.FATURAS_GRAPHQL);
        }
        
        boolean todasPermitidas = true;
        
        for (final String entidade : entidadesParaValidar) {
            // CORRIGIDO: Usa o tamanho do BLOCO (30 dias) para determinar a regra de limitação
            // Isso permite que blocos de 30 dias usem a regra de "sem limite" em vez de 12 horas
            final ValidadorLimiteExtracao.ResultadoValidacao resultado = 
                validador.validarLimiteExtracao(entidade, bloco.dataInicio, bloco.dataFim);
            
            if (!resultado.isPermitido()) {
                log.warn("⏸️ {}: {}", entidade, resultado.getMotivo());
                log.console("   ⏳ Aguarde {} hora(s) antes de tentar novamente", resultado.getHorasRestantes());
                todasPermitidas = false;
            }
        }
        
        return todasPermitidas;
    }
    
    /**
     * Classe auxiliar para representar um bloco de período.
     */
    private static class BlocoPeriodo {
        final LocalDate dataInicio;
        final LocalDate dataFim;
        
        BlocoPeriodo(final LocalDate dataInicio, final LocalDate dataFim) {
            this.dataInicio = dataInicio;
            this.dataFim = dataFim;
        }
    }
}

