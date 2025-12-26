package br.com.extrator.auditoria.servicos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.auditoria.enums.StatusAuditoria;
import br.com.extrator.auditoria.enums.StatusValidacao;
import br.com.extrator.auditoria.modelos.ResultadoAuditoria;
import br.com.extrator.auditoria.modelos.ResultadoValidacaoEntidade;
import br.com.extrator.auditoria.relatorios.AuditoriaRelatorio;
import br.com.extrator.auditoria.validacao.AuditoriaValidator;
import br.com.extrator.util.banco.GerenciadorConexao;
import br.com.extrator.util.validacao.ConstantesEntidades;

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
    private final CompletudeValidator completudeValidator; // ← NOVO

    public AuditoriaService() {
        this.validator = new AuditoriaValidator();
        this.relatorio = new AuditoriaRelatorio();
        this.completudeValidator = new CompletudeValidator(); // ← NOVO
    }

    /**
     * Executa auditoria completa dos dados extraídos nas últimas 24 horas.
     * 
     * @return ResultadoAuditoria com o resultado da auditoria
     */
    public ResultadoAuditoria executarAuditoriaCompleta() {
        logger.info("🔍 Iniciando auditoria completa dos dados extraídos");

        final Instant agora = Instant.now();
        final Instant inicio24h = agora.minusSeconds(24 * 60 * 60); // 24 horas em segundos

        return executarAuditoriaPorPeriodo(inicio24h, agora);
    }

    /**
     * Executa auditoria dos dados extraídos em um período específico.
     * 
     * @param dataInicio Data de início do período
     * @param dataFim    Data de fim do período
     * @return ResultadoAuditoria com o resultado da auditoria
     */
    public ResultadoAuditoria executarAuditoriaPorPeriodo(final Instant dataInicio, final Instant dataFim) {
        final ResultadoAuditoria resultado = new ResultadoAuditoria();
        resultado.setDataInicio(dataInicio);
        resultado.setDataFim(dataFim);
        resultado.setDataExecucao(Instant.now());

        try (Connection conexao = GerenciadorConexao.obterConexao()) {
            // ✅ FASE 1: Buscar API (com fallback)
            Map<String, Integer> totaisApi = null;
            try {
                logger.info("🔍 Buscando API...");
                final Optional<Map<String, Integer>> totaisApiOpt = completudeValidator.buscarTotaisEslCloud(java.time.LocalDate.now());
                totaisApi = totaisApiOpt.orElse(null);
            } catch (final Exception e) {
                logger.error("❌ API falhou: {} - Continuando local", e.getMessage());
            }

            // ✅ FASE 2: Criar todas as tabelas antes de validar
            logger.info("🔧 Garantindo que todas as tabelas existem...");
            validator.criarTodasTabelasSeNaoExistirem(conexao);
            
            // ✅ FASE 3: Validar com comparação
            final List<String> ents = List.of(
                ConstantesEntidades.COTACOES,
                ConstantesEntidades.COLETAS,
                ConstantesEntidades.CONTAS_A_PAGAR,
                ConstantesEntidades.FATURAS_POR_CLIENTE,
                ConstantesEntidades.FRETES,
                ConstantesEntidades.FATURAS_GRAPHQL,
                ConstantesEntidades.MANIFESTOS,
                ConstantesEntidades.LOCALIZACAO_CARGAS
            );

            for (final String e : ents) {
                try {
                    ResultadoValidacaoEntidade v;
                    if (totaisApi != null && totaisApi.containsKey(e)) {
                        // Comparar banco vs API
                        final long banco = contarRegistrosNoBanco(conexao, e);
                        final int esperado = totaisApi.get(e);

                        if (banco == esperado) {
                            // ✅ Dados completos
                            v = ResultadoValidacaoEntidade.completo(e, esperado, (int) banco);
                            logger.info("✅ {} - COMPLETO: {}/{} registros (100%)", e, banco, esperado);
                        } else if (banco < esperado) {
                            final double perc = (banco * 100.0) / esperado;
                            if (perc >= 80.0) {
                                v = ResultadoValidacaoEntidade.ok(e, banco);
                                v.adicionarObservacao(String.format("Diferença aceitável (Deduplicação): %.1f%% de match", perc));
                                logger.info("✅ {} - OK (Deduplicado): {}/{} registros", e, banco, esperado);
                            } else {
                                v = ResultadoValidacaoEntidade.incompleto(e, esperado, (int) banco);
                                logger.warn("❌ {} - INCOMPLETO: {}/{} registros ({:.1f}% - faltam {})",
                                        e, banco, esperado, perc, esperado - banco);
                            }
                        } else {
                            // ⚠️ Dados duplicados
                            v = ResultadoValidacaoEntidade.duplicados(e, esperado, (int) banco);
                            logger.warn("⚠️ {} - DUPLICADOS: {} registros (esperado: {}, excesso: {})",
                                    e, banco, esperado, banco - esperado);
                        }
                    } else {
                        v = validator.validarEntidade(conexao, e, dataInicio, dataFim);
                    }
                    resultado.adicionarValidacao(e, v);
                } catch (SQLException | RuntimeException ex) {
                    resultado.adicionarValidacao(e, ResultadoValidacaoEntidade.erro(e, 0, ex.getMessage()));
                }
            }

            // ✅ SEMPRE gerar relatório
            resultado.determinarStatusGeral();
            relatorio.gerarRelatorio(resultado);

        } catch (SQLException | RuntimeException ex) {
            resultado.setErro(ex.getMessage());
            resultado.setStatusGeral(StatusAuditoria.ERRO);
            try {
                relatorio.gerarRelatorio(resultado);
            } catch (final Exception e) {
            }
        }

        return resultado;
    }

    // ✅ NOVO MÉTODO
    private long contarRegistrosNoBanco(final Connection c, final String e)
            throws SQLException {
        final String tabela = mapearTabela(e);
        validator.criarTabelaSeNaoExistir(c, tabela);
        
        // Para Contas a Pagar, usar issue_date (data de emissão) em vez de data_extracao
        // porque a API filtra por issue_date, então devemos contar da mesma forma
        final String sql;
        if (ConstantesEntidades.CONTAS_A_PAGAR.equals(e)) {
            sql = String.format("SELECT COUNT(*) FROM %s WHERE issue_date >= DATEADD(day, -1, CAST(GETDATE() AS DATE))", tabela);
        } else {
            sql = String.format("SELECT COUNT(*) FROM %s WHERE data_extracao >= DATEADD(hour, -24, GETDATE())", tabela);
        }
        
        try (PreparedStatement s = c.prepareStatement(sql);
             ResultSet r = s.executeQuery()) {
            return r.next() ? r.getLong(1) : 0;
        } catch (final SQLException ex) {
            // Fallback para entidades que podem usar created_at
            final String sql2 = String.format("SELECT COUNT(*) FROM %s WHERE created_at >= DATEADD(hour, -24, GETDATE())", tabela);
            try (PreparedStatement s = c.prepareStatement(sql2);
                 ResultSet r = s.executeQuery()) {
                return r.next() ? r.getLong(1) : 0;
            }
        }
    }

    private String mapearTabela(final String entidade) {
        return switch (entidade) {
            case "faturas_a_pagar_data_export" -> ConstantesEntidades.CONTAS_A_PAGAR;
            default -> entidade;
        };
    }

    /**
     * Executa auditoria rápida apenas verificando se existem dados recentes.
     * 
     * @return true se existem dados das últimas 24 horas, false caso contrário
     */
    public boolean executarAuditoriaRapida() {
        logger.info("⚡ Executando auditoria rápida");

        try (final Connection conexao = GerenciadorConexao.obterConexao()) {
            final Instant agora = Instant.now();
            final Instant inicio24h = agora.minusSeconds(24 * 60 * 60); // 24 horas em segundos

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
            final Instant agora = Instant.now();
            final Instant inicio24h = agora.minusSeconds(24 * 60 * 60);

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
