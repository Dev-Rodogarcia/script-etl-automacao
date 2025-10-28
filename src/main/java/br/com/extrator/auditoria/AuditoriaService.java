package br.com.extrator.auditoria;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.util.GerenciadorConexao;

/**
 * Serviço principal de auditoria que coordena a validação da completude
 * dos dados extraídos das APIs do ESL Cloud.
 * 
 * Verifica se todas as entidades foram extraídas corretamente e gera
 * relatórios de auditoria para identificar possíveis inconsistências.
 */
public class AuditoriaService {
    private static final Logger logger = LoggerFactory.getLogger(AuditoriaService.class);
    
    private final AuditoriaValidator validator;
    private final AuditoriaRelatorio relatorio;
    
    public AuditoriaService() {
        this.validator = new AuditoriaValidator();
        this.relatorio = new AuditoriaRelatorio();
    }
    
    /**
     * Executa auditoria completa dos dados extraídos nas últimas 24 horas.
     * 
     * @return ResultadoAuditoria com o resultado da auditoria
     */
    public ResultadoAuditoria executarAuditoriaCompleta() {
        logger.info("🔍 Iniciando auditoria completa dos dados extraídos");
        
        final LocalDateTime agora = LocalDateTime.now();
        final LocalDateTime inicio24h = agora.minusHours(24);
        
        return executarAuditoriaPorPeriodo(inicio24h, agora);
    }
    
    /**
     * Executa auditoria dos dados extraídos em um período específico.
     * 
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return ResultadoAuditoria com o resultado da auditoria
     */
    public ResultadoAuditoria executarAuditoriaPorPeriodo(final LocalDateTime dataInicio, final LocalDateTime dataFim) {
        logger.info("🔍 Executando auditoria para período: {} até {}", 
                   dataInicio.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                   dataFim.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        final ResultadoAuditoria resultado = new ResultadoAuditoria();
        resultado.setDataInicio(dataInicio);
        resultado.setDataFim(dataFim);
        resultado.setDataExecucao(LocalDateTime.now());
        
        try (final Connection conexao = GerenciadorConexao.obterConexao()) {
            // Validar cada entidade
            final List<String> entidades = List.of(
                "cotacoes", "coletas", "faturas_a_pagar", "faturas_a_receber", 
                "fretes", "manifestos", "ocorrencias", "localizacao_cargas"
            );
            
            for (final String entidade : entidades) {
                logger.debug("Validando entidade: {}", entidade);
                final ResultadoValidacaoEntidade validacao = validator.validarEntidade(conexao, entidade, dataInicio, dataFim);
                resultado.adicionarValidacao(entidade, validacao);
            }
            
            // Gerar relatório
            relatorio.gerarRelatorio(resultado);
            
            // Determinar status geral
            resultado.determinarStatusGeral();
            
            logger.info("✅ Auditoria concluída. Status: {}", resultado.getStatusGeral());
            
        } catch (final SQLException e) {
            logger.error("❌ Erro durante auditoria: {}", e.getMessage(), e);
            resultado.setErro("Erro de conexão com banco de dados: " + e.getMessage());
            resultado.setStatusGeral(StatusAuditoria.ERRO);
        } catch (final Exception e) {
            logger.error("❌ Erro inesperado durante auditoria: {}", e.getMessage(), e);
            resultado.setErro("Erro inesperado: " + e.getMessage());
            resultado.setStatusGeral(StatusAuditoria.ERRO);
        }
        
        return resultado;
    }
    
    /**
     * Executa auditoria rápida apenas verificando se existem dados recentes.
     * 
     * @return true se existem dados das últimas 24 horas, false caso contrário
     */
    public boolean executarAuditoriaRapida() {
        logger.info("⚡ Executando auditoria rápida");
        
        try (final Connection conexao = GerenciadorConexao.obterConexao()) {
            final LocalDateTime agora = LocalDateTime.now();
            final LocalDateTime inicio24h = agora.minusHours(24);
            
            return validator.verificarExistenciaDadosRecentes(conexao, inicio24h, agora);
            
        } catch (final SQLException e) {
            logger.error("❌ Erro durante auditoria rápida: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Valida uma entidade específica.
     * 
     * @param nomeEntidade Nome da entidade a ser validada
     * @return ResultadoValidacaoEntidade com o resultado da validação
     */
    public ResultadoValidacaoEntidade validarEntidadeEspecifica(final String nomeEntidade) {
        logger.info("🔍 Validando entidade específica: {}", nomeEntidade);
        
        try (final Connection conexao = GerenciadorConexao.obterConexao()) {
            final LocalDateTime agora = LocalDateTime.now();
            final LocalDateTime inicio24h = agora.minusHours(24);
            
            return validator.validarEntidade(conexao, nomeEntidade, inicio24h, agora);
            
        } catch (final SQLException e) {
            logger.error("❌ Erro ao validar entidade {}: {}", nomeEntidade, e.getMessage(), e);
            final ResultadoValidacaoEntidade resultado = new ResultadoValidacaoEntidade();
            resultado.setNomeEntidade(nomeEntidade);
            resultado.setErro("Erro de conexão: " + e.getMessage());
            resultado.setStatus(StatusValidacao.ERRO);
            return resultado;
        }
    }
}