package br.com.extrator.servicos;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
                    .map(dto -> faturaAPagarMapper.toEntity(dto, "{}"))
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
     * Executa a extração completa de todas as entidades para uma data específica.
     * Versão resiliente que não interrompe o processo em caso de falhas individuais.
     * 
     * @param dataInicio Data para extração
     */
    public void executarExtracaoCompleta(LocalDate dataInicio) {
        logger.info("🔄 Iniciando extração completa para data: {}", dataInicio);
        
        int ok = 0, fail = 0;
        List<String> erros = new ArrayList<>();
        
        // Faturas a Receber
        try {
            extrairFaturasAReceber(dataInicio);
            ok++;
            logger.info("✅ Faturas a receber extraídas com sucesso");
        } catch (Exception e) {
            fail++;
            erros.add("faturas_a_receber");
            logger.error("❌ Erro na extração de faturas a receber: {}", e.getMessage(), e);
        }
        
        // Faturas a Pagar
        try {
            extrairFaturasAPagar(dataInicio);
            ok++;
            logger.info("✅ Faturas a pagar extraídas com sucesso");
        } catch (Exception e) {
            fail++;
            erros.add("faturas_a_pagar");
            logger.error("❌ Erro na extração de faturas a pagar: {}", e.getMessage(), e);
        }
        
        // Ocorrências
        try {
            extrairOcorrencias(dataInicio);
            ok++;
            logger.info("✅ Ocorrências extraídas com sucesso");
        } catch (Exception e) {
            fail++;
            erros.add("ocorrencias");
            logger.error("❌ Erro na extração de ocorrências: {}", e.getMessage(), e);
        }
        
        // Fretes (via GraphQL Runner)
        try {
            br.com.extrator.runners.GraphQLRunner.executar(dataInicio);
            ok++;
            logger.info("✅ Fretes extraídos com sucesso");
        } catch (Exception e) {
            fail++;
            erros.add("fretes");
            logger.error("❌ Erro na extração de fretes: {}", e.getMessage(), e);
        }
        
        // Coletas (via GraphQL Runner - já incluído no GraphQLRunner)
        try {
            // Coletas são extraídas junto com fretes no GraphQLRunner
            // Incrementamos apenas se fretes foi bem-sucedido
            if (!erros.contains("fretes")) {
                ok++;
                logger.info("✅ Coletas extraídas com sucesso");
            } else {
                fail++;
                erros.add("coletas");
            }
        } catch (Exception e) {
            fail++;
            erros.add("coletas");
            logger.error("❌ Erro na extração de coletas: {}", e.getMessage(), e);
        }
        
        // Manifestos (via DataExport Runner)
        try {
            br.com.extrator.runners.DataExportRunner.executar(dataInicio);
            ok++;
            logger.info("✅ Manifestos extraídos com sucesso");
        } catch (Exception e) {
            fail++;
            erros.add("manifestos");
            logger.error("❌ Erro na extração de manifestos: {}", e.getMessage(), e);
        }
        
        // Cotações (via DataExport Runner - já incluído no DataExportRunner)
        try {
            // Cotações são extraídas junto com manifestos no DataExportRunner
            if (!erros.contains("manifestos")) {
                ok++;
                logger.info("✅ Cotações extraídas com sucesso");
            } else {
                fail++;
                erros.add("cotacoes");
            }
        } catch (Exception e) {
            fail++;
            erros.add("cotacoes");
            logger.error("❌ Erro na extração de cotações: {}", e.getMessage(), e);
        }
        
        // Localizações (via DataExport Runner - já incluído no DataExportRunner)
        try {
            // Localizações são extraídas junto com manifestos no DataExportRunner
            if (!erros.contains("manifestos")) {
                ok++;
                logger.info("✅ Localizações extraídas com sucesso");
            } else {
                fail++;
                erros.add("localizacoes");
            }
        } catch (Exception e) {
            fail++;
            erros.add("localizacoes");
            logger.error("❌ Erro na extração de localizações: {}", e.getMessage(), e);
        }
        
        logger.info("🏁 Extração completa finalizada - Sucessos: {}/3 REST + 2 GraphQL + 3 DataExport, Falhas: {} -> {}", ok, fail, erros);
    }
}