package br.com.extrator.servicos;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.api.ClienteApiRest;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.FaturaAPagarEntity;
import br.com.extrator.db.entity.FaturaAReceberEntity;
import br.com.extrator.db.entity.LogExtracaoEntity;
import br.com.extrator.db.entity.LogExtracaoEntity.StatusExtracao;
import br.com.extrator.db.entity.OcorrenciaEntity;
import br.com.extrator.db.repository.FaturaAPagarRepository;
import br.com.extrator.db.repository.FaturaAReceberRepository;
import br.com.extrator.db.repository.LogExtracaoRepository;
import br.com.extrator.db.repository.OcorrenciaRepository;
import br.com.extrator.modelo.rest.faturaspagar.FaturaAPagarDTO;
import br.com.extrator.modelo.rest.faturaspagar.FaturaAPagarMapper;
import br.com.extrator.modelo.rest.faturasreceber.FaturaAReceberDTO;
import br.com.extrator.modelo.rest.faturasreceber.FaturaAReceberMapper;
import br.com.extrator.modelo.rest.ocorrencias.OcorrenciaDTO;
import br.com.extrator.modelo.rest.ocorrencias.OcorrenciaMapper;

/**
 * Serviço responsável por coordenar as extrações de dados e registrar
 * o status de cada extração na tabela log_extracoes.
 * 
 * Este serviço resolve o problema crítico de auditoria ao garantir que
 * todas as extrações sejam registradas com seu status de completude.
 */
public class ExtracaoServico {
    
    private static final Logger logger = LoggerFactory.getLogger(ExtracaoServico.class);
    
    private final ClienteApiRest clienteApiRest;
    private final LogExtracaoRepository logExtracaoRepository;
    
    // Repositórios
    private final FaturaAReceberRepository faturaAReceberRepository;
    private final FaturaAPagarRepository faturaAPagarRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    
    // Mappers
    private final FaturaAReceberMapper faturaAReceberMapper;
    private final FaturaAPagarMapper faturaAPagarMapper;
    private final OcorrenciaMapper ocorrenciaMapper;
    
    public ExtracaoServico() {
        this.clienteApiRest = new ClienteApiRest();
        this.logExtracaoRepository = new LogExtracaoRepository();
        
        this.faturaAReceberRepository = new FaturaAReceberRepository();
        this.faturaAPagarRepository = new FaturaAPagarRepository();
        this.ocorrenciaRepository = new OcorrenciaRepository();
        
        this.faturaAReceberMapper = new FaturaAReceberMapper();
        this.faturaAPagarMapper = new FaturaAPagarMapper();
        this.ocorrenciaMapper = new OcorrenciaMapper();
        
        // Garante que a tabela log_extracoes existe
        this.logExtracaoRepository.criarTabelaSeNaoExistir();
    }
    
    /**
     * Executa a extração de faturas a receber com logging de status
     */
    public void extrairFaturasAReceber(LocalDate dataInicio) {
        String entidade = "faturas_a_receber";
        LocalDateTime timestampInicio = LocalDateTime.now();
        
        try {
            logger.info("Iniciando extração de {}", entidade);
            
            ResultadoExtracao<FaturaAReceberDTO> resultado = clienteApiRest.buscarFaturasAReceber(dataInicio);
            List<FaturaAReceberDTO> dtos = resultado.getDados();
            
            // Processa e salva os dados
            int registrosSalvos = 0;
            if (!dtos.isEmpty()) {
                List<FaturaAReceberEntity> entities = dtos.stream()
                    .map(faturaAReceberMapper::toEntity)
                    .collect(Collectors.toList());
                registrosSalvos = faturaAReceberRepository.salvar(entities);
            }
            
            // Determina o status final
            StatusExtracao statusFinal = resultado.isCompleto() ? 
                StatusExtracao.COMPLETO : StatusExtracao.INCOMPLETO_LIMITE;
            
            String mensagem = resultado.isCompleto() ? 
                "Extração completa" : 
                "Extração incompleta: " + resultado.getMotivoInterrupcao();
            
            // Registra o log
            LogExtracaoEntity logExtracao = new LogExtracaoEntity(
                entidade,
                timestampInicio,
                LocalDateTime.now(),
                statusFinal,
                registrosSalvos,
                resultado.getPaginasProcessadas(),
                mensagem
            );
            
            logExtracaoRepository.gravarLogExtracao(logExtracao);
            
            logger.info("Extração de {} finalizada: {} registros, status: {}", 
                entidade, registrosSalvos, statusFinal);
                
        } catch (RuntimeException | SQLException e) {
            logger.error("Erro na extração de {}: {}", entidade, e.getMessage(), e);
            
            // Registra o erro no log
            LogExtracaoEntity logErro = new LogExtracaoEntity(
                entidade,
                timestampInicio,
                LocalDateTime.now(),
                StatusExtracao.ERRO_API,
                0,
                0,
                "Erro: " + e.getMessage()
            );
            
            logExtracaoRepository.gravarLogExtracao(logErro);
            throw new RuntimeException("Falha na extração de " + entidade, e);
        }
    }
    
    /**
     * Executa a extração de faturas a pagar com logging de status
     */
    public void extrairFaturasAPagar(LocalDate dataInicio) {
        String entidade = "faturas_a_pagar";
        LocalDateTime timestampInicio = LocalDateTime.now();
        
        try {
            logger.info("Iniciando extração de {}", entidade);
            
            ResultadoExtracao<FaturaAPagarDTO> resultado = clienteApiRest.buscarFaturasAPagar(dataInicio);
            List<FaturaAPagarDTO> dtos = resultado.getDados();
            
            // Processa e salva os dados
            int registrosSalvos = 0;
            if (!dtos.isEmpty()) {
                List<FaturaAPagarEntity> entities = dtos.stream()
                    .map(dto -> {
                        // Busca os itens/parcelas da fatura específica
                        String itensJson = clienteApiRest.buscarItensFaturaAPagar(dto.getId());
                        return faturaAPagarMapper.toEntity(dto, itensJson);
                    })
                    .collect(Collectors.toList());
                registrosSalvos = faturaAPagarRepository.salvar(entities);
            }
            
            // Determina o status final
            StatusExtracao statusFinal = resultado.isCompleto() ? 
                StatusExtracao.COMPLETO : StatusExtracao.INCOMPLETO_LIMITE;
            
            String mensagem = resultado.isCompleto() ? 
                "Extração completa" : 
                "Extração incompleta: " + resultado.getMotivoInterrupcao();
            
            // Registra o log
            LogExtracaoEntity logExtracao = new LogExtracaoEntity(
                entidade,
                timestampInicio,
                LocalDateTime.now(),
                statusFinal,
                registrosSalvos,
                resultado.getPaginasProcessadas(),
                mensagem
            );
            
            logExtracaoRepository.gravarLogExtracao(logExtracao);
            
            logger.info("Extração de {} finalizada: {} registros, status: {}", 
                entidade, registrosSalvos, statusFinal);
                
        } catch (RuntimeException | SQLException e) {
            logger.error("Erro na extração de {}: {}", entidade, e.getMessage(), e);
            
            // Registra o erro no log
            LogExtracaoEntity logErro = new LogExtracaoEntity(
                entidade,
                timestampInicio,
                LocalDateTime.now(),
                StatusExtracao.ERRO_API,
                0,
                0,
                "Erro: " + e.getMessage()
            );
            
            logExtracaoRepository.gravarLogExtracao(logErro);
            throw new RuntimeException("Falha na extração de " + entidade, e);
        }
    }
    
    /**
     * Executa a extração de ocorrências com logging de status
     */
    public void extrairOcorrencias(LocalDate dataInicio) {
        String entidade = "ocorrencias";
        LocalDateTime timestampInicio = LocalDateTime.now();
        
        try {
            logger.info("Iniciando extração de {}", entidade);
            
            ResultadoExtracao<OcorrenciaDTO> resultado = clienteApiRest.buscarOcorrencias(dataInicio);
            List<OcorrenciaDTO> dtos = resultado.getDados();
            
            // Processa e salva os dados
            int registrosSalvos = 0;
            if (!dtos.isEmpty()) {
                List<OcorrenciaEntity> entities = dtos.stream()
                    .map(ocorrenciaMapper::toEntity)
                    .collect(Collectors.toList());
                registrosSalvos = ocorrenciaRepository.salvar(entities);
            }
            
            // Determina o status final
            StatusExtracao statusFinal = resultado.isCompleto() ? 
                StatusExtracao.COMPLETO : StatusExtracao.INCOMPLETO_LIMITE;
            
            String mensagem = resultado.isCompleto() ? 
                "Extração completa" : 
                "Extração incompleta: " + resultado.getMotivoInterrupcao();
            
            // Registra o log
            LogExtracaoEntity logExtracao = new LogExtracaoEntity(
                entidade,
                timestampInicio,
                LocalDateTime.now(),
                statusFinal,
                registrosSalvos,
                resultado.getPaginasProcessadas(),
                mensagem
            );
            
            logExtracaoRepository.gravarLogExtracao(logExtracao);
            
            logger.info("Extração de {} finalizada: {} registros, status: {}", 
                entidade, registrosSalvos, statusFinal);
                
        } catch (RuntimeException | SQLException e) {
            logger.error("Erro na extração de {}: {}", entidade, e.getMessage(), e);
            
            // Registra o erro no log
            LogExtracaoEntity logErro = new LogExtracaoEntity(
                entidade,
                timestampInicio,
                LocalDateTime.now(),
                StatusExtracao.ERRO_API,
                0,
                0,
                "Erro: " + e.getMessage()
            );
            
            logExtracaoRepository.gravarLogExtracao(logErro);
            throw new RuntimeException("Falha na extração de " + entidade, e);
        }
    }
    
    /**
     * Executa todas as extrações REST com logging
     */
    public void executarExtracaoCompleta(LocalDate dataInicio) {
        logger.info("Iniciando extração completa para data: {}", dataInicio);
        
        try {
            extrairFaturasAReceber(dataInicio);
            Thread.sleep(2000); // Rate limit
            
            extrairFaturasAPagar(dataInicio);
            Thread.sleep(2000); // Rate limit
            
            extrairOcorrencias(dataInicio);
            
            logger.info("Extração completa finalizada com sucesso");
            
        } catch (RuntimeException | InterruptedException e) {
            logger.error("Erro na extração completa: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na extração completa", e);
        }
    }
}