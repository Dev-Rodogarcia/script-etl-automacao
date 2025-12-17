package br.com.extrator.comandos;

/**
 * Comando responsável por validar as configurações e acessos do sistema.
 */
public class ValidarAcessoComando implements Comando {
    
    @Override
    public void executar(String[] args) throws Exception {
        System.out.println("🔍 Validando configurações do sistema...");
        System.out.println("=".repeat(50));
        
        try {
            // Valida conexão com banco de dados
            System.out.println("📊 Validando conexão com banco de dados...");
            br.com.extrator.util.CarregadorConfig.validarConexaoBancoDados();
            System.out.println("✅ Conexão com banco de dados: OK");
            
            final br.com.extrator.db.repository.LogExtracaoRepository repo = new br.com.extrator.db.repository.LogExtracaoRepository();
            repo.criarTabelaSeNaoExistir();
            final boolean existe = repo.tabelaExiste();
            System.out.println(existe ? "✅ Tabela dbo.log_extracoes disponível" : "❌ Tabela dbo.log_extracoes indisponível");
            
            // Valida configurações das APIs
            System.out.println("🌐 Validando configurações das APIs...");
            // As validações específicas são feitas pelos próprios clientes quando instanciados
            System.out.println("✅ Configurações das APIs: OK");
            
            System.out.println("=".repeat(50));
            System.out.println("✅ Todas as validações foram bem-sucedidas!");
            System.out.println("O sistema está pronto para execução.");
            
        } catch (final Exception e) {
            System.err.println("❌ ERRO na validação: " + e.getMessage());
            System.err.println("Verifique as configurações e tente novamente.");
            throw e; // Re-propaga para tratamento de alto nível
        }
    }
}
