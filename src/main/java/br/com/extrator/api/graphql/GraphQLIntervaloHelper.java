package br.com.extrator.api.graphql;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.api.ResultadoExtracao;

/**
 * Helper para execução de queries GraphQL por intervalo de datas.
 * Útil para entidades que não suportam intervalo diretamente (ex: coletas, faturas).
 */
public final class GraphQLIntervaloHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphQLIntervaloHelper.class);
    
    private GraphQLIntervaloHelper() {
        // Construtor privado para classe utilitária
    }
    
    /**
     * Executa uma função de extração para cada dia do intervalo especificado.
     * Consolida os resultados em um único ResultadoExtracao.
     * 
     * @param <T> Tipo do DTO retornado
     * @param dataInicio Data de início do período (inclusive)
     * @param dataFim Data de fim do período (inclusive)
     * @param executorDia Função que executa a extração para um dia específico
     * @param nomeEntidade Nome da entidade para logs
     * @return ResultadoExtracao consolidado de todos os dias
     */
    public static <T> ResultadoExtracao<T> executarPorDia(
            final LocalDate dataInicio,
            final LocalDate dataFim,
            final Function<LocalDate, ResultadoExtracao<T>> executorDia,
            final String nomeEntidade) {
        
        logger.info("🔍 Buscando {} via GraphQL - Período: {} a {}", nomeEntidade, dataInicio, dataFim);
        
        final List<T> todas = new ArrayList<>();
        int totalPaginas = 0;
        boolean todasCompletas = true;
        
        LocalDate dia = dataInicio;
        int diaAtual = 1;
        final long totalDias = ChronoUnit.DAYS.between(dataInicio, dataFim) + 1;
        
        while (!dia.isAfter(dataFim)) {
            logger.info("🔍 {} - Dia {}/{}: {}", nomeEntidade, diaAtual, totalDias, dia);
            
            final ResultadoExtracao<T> resultadoDia = executorDia.apply(dia);
            todas.addAll(resultadoDia.getDados());
            totalPaginas += resultadoDia.getPaginasProcessadas();
            
            if (resultadoDia.isCompleto()) {
                logger.info("✅ Dia {}/{}: {} registros", diaAtual, totalDias, resultadoDia.getDados().size());
            } else {
                logger.warn("⚠️ Dia {}/{}: {} registros (INCOMPLETO)", diaAtual, totalDias, resultadoDia.getDados().size());
                todasCompletas = false;
            }
            
            dia = dia.plusDays(1);
            diaAtual++;
        }
        
        logger.info("✅ Total: {} {} extraídos", todas.size(), nomeEntidade);
        
        if (todasCompletas) {
            return ResultadoExtracao.completo(todas, totalPaginas, todas.size());
        } else {
            return ResultadoExtracao.incompleto(todas, 
                ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS, 
                totalPaginas, todas.size());
        }
    }
    
    /**
     * Executa uma função de extração para cada dia de um intervalo,
     * usando um executor que recebe data e variáveis adicionais.
     * 
     * @param <T> Tipo do DTO retornado
     * @param dataInicio Data de início do período (inclusive)
     * @param dataFim Data de fim do período (inclusive)
     * @param executorDia Função que executa a extração para um dia específico
     * @param nomeEntidade Nome da entidade para logs
     * @param logProgresso Se true, exibe log para cada dia
     * @return ResultadoExtracao consolidado de todos os dias
     */
    public static <T> ResultadoExtracao<T> executarPorDia(
            final LocalDate dataInicio,
            final LocalDate dataFim,
            final Function<LocalDate, ResultadoExtracao<T>> executorDia,
            final String nomeEntidade,
            final boolean logProgresso) {
        
        if (logProgresso) {
            return executarPorDia(dataInicio, dataFim, executorDia, nomeEntidade);
        }
        
        // Versão silenciosa sem logs de progresso por dia
        final List<T> todas = new ArrayList<>();
        int totalPaginas = 0;
        boolean todasCompletas = true;
        
        LocalDate dia = dataInicio;
        
        while (!dia.isAfter(dataFim)) {
            final ResultadoExtracao<T> resultadoDia = executorDia.apply(dia);
            todas.addAll(resultadoDia.getDados());
            totalPaginas += resultadoDia.getPaginasProcessadas();
            
            if (!resultadoDia.isCompleto()) {
                todasCompletas = false;
            }
            
            dia = dia.plusDays(1);
        }
        
        if (todasCompletas) {
            return ResultadoExtracao.completo(todas, totalPaginas, todas.size());
        } else {
            return ResultadoExtracao.incompleto(todas, 
                ResultadoExtracao.MotivoInterrupcao.LIMITE_PAGINAS, 
                totalPaginas, todas.size());
        }
    }
}

