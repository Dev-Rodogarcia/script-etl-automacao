package br.com.extrator.runners.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.extrator.db.repository.LogExtracaoRepository;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Classe utilitária para operações comuns de extração.
 * Centraliza lógica duplicada entre GraphQLExtractionService e DataExportExtractionService.
 *
 * Inclui avisos de segurança (timeouts, etc.) para o resumo final nos logs.
 *
 * @author Sistema de Extração ESL Cloud
 * @version 1.0
 */
public final class ExtractionHelper {

    private static final ThreadLocal<List<String>> AVISOS_SEGURANCA = ThreadLocal.withInitial(ArrayList::new);

    private ExtractionHelper() {
        // Impede instanciação
    }

    /**
     * Registra um aviso de segurança (ex.: timeout, interrupção) para ser exibido
     * no resumo final (EVENTOS / OBSERVAÇÕES) dos logs.
     */
    public static void appendAvisoSeguranca(final String mensagem) {
        if (mensagem != null && !mensagem.isBlank()) {
            AVISOS_SEGURANCA.get().add(mensagem);
        }
    }

    /**
     * Retorna e limpa a lista de avisos de segurança da thread atual.
     * Usado no resumo consolidado ao final da extração.
     */
    public static List<String> drenarAvisosSeguranca() {
        final List<String> avisos = AVISOS_SEGURANCA.get();
        final List<String> copia = new ArrayList<>(avisos);
        avisos.clear();
        return Collections.unmodifiableList(copia);
    }

    /**
     * Limpa os avisos de segurança da thread atual. Chamar no início de execute().
     */
    public static void limparAvisosSeguranca() {
        AVISOS_SEGURANCA.get().clear();
    }
    
    /**
     * Executa uma extração com logging e tratamento de erros padronizado.
     * 
     * @param extractor Extractor a ser executado
     * @param logger Logger para execução
     * @param logRepository Repository para gravar logs
     * @param dataInicio Data de início
     * @param dataFim Data de fim
     * @param nomeEntidade Nome da entidade para mensagens de erro
     * @throws RuntimeException Se a extração falhar
     */
    public static <T> void executarExtracao(
            final EntityExtractor<T> extractor,
            final ExtractionLogger logger,
            final LogExtracaoRepository logRepository,
            final LocalDate dataInicio,
            final LocalDate dataFim,
            final String nomeEntidade) {
        
        final ExtractionResult result = logger.executeWithLogging(extractor, dataInicio, dataFim, extractor.getEmoji());
        logRepository.gravarLogExtracao(result.toLogEntity());
        
        if (!result.isSucesso()) {
            throw new RuntimeException(String.format(ConstantesExtracao.MSG_ERRO_EXTRACAO, nomeEntidade), result.getErro());
        }
    }
    
    /**
     * Aplica delay entre extrações com tratamento de InterruptedException.
     * 
     * @throws RuntimeException Se o delay for interrompido
     */
    public static void aplicarDelay() {
        try {
            ConstantesEntidades.aplicarDelayEntreExtracoes();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ConstantesExtracao.MSG_ERRO_DELAY_INTERROMPIDO, e);
        }
    }
}
