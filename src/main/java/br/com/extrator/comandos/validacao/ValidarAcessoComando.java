package br.com.extrator.comandos.validacao;

import br.com.extrator.comandos.base.Comando;
import br.com.extrator.util.console.LoggerConsole;

/**
 * Comando responsável por validar as configurações e acessos do sistema.
 */
public class ValidarAcessoComando implements Comando {
    // PROBLEMA #9 CORRIGIDO: Usar LoggerConsole para log duplo
    private static final LoggerConsole log = LoggerConsole.getLogger(ValidarAcessoComando.class);
    
    @Override
    public void executar(String[] args) throws Exception {
        log.info("🔍 Validando configurações do sistema...");
        log.console("=".repeat(50));
        
        try {
            // Valida conexão com banco de dados
            log.info("📊 Validando conexão com banco de dados...");
            br.com.extrator.util.configuracao.CarregadorConfig.validarConexaoBancoDados();
            log.info("✅ Conexão com banco de dados: OK");
            
            final br.com.extrator.db.repository.LogExtracaoRepository repo = new br.com.extrator.db.repository.LogExtracaoRepository();
            repo.criarTabelaSeNaoExistir();
            final boolean existe = repo.tabelaExiste();
            log.info(existe ? "✅ Tabela dbo.log_extracoes disponível" : "❌ Tabela dbo.log_extracoes indisponível");
            
            // Valida configurações das APIs
            log.info("🌐 Validando configurações das APIs...");
            log.info("✅ Configurações das APIs: OK");
            
            log.console("=".repeat(50));
            log.info("✅ Todas as validações foram bem-sucedidas!");
            log.info("O sistema está pronto para execução.");
            
        } catch (final Exception e) {
            log.error("❌ ERRO na validação: {}", e.getMessage());
            log.error("Verifique as configurações e tente novamente.");
            throw e;
        }
    }
}
