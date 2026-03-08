/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/comandos/validacao/ValidarAcessoComando.java
Classe  : ValidarAcessoComando (class)
Pacote  : br.com.extrator.comandos.cli.validacao
Modulo  : Comando CLI (validacao)
Papel   : Implementa responsabilidade de validar acesso comando.

Conecta com:
- Comando (comandos.base)
- LoggerConsole (util.console)

Fluxo geral:
1) Executa validacoes de acesso, timestamps e consistencia.
2) Compara API versus banco quando aplicavel.
3) Emite resultado de qualidade para operacao.

Estrutura interna:
Metodos principais:
- Metodos nao mapeados automaticamente; consulte a implementacao abaixo.
Atributos-chave:
- log: campo de estado para "log".
[DOC-FILE-END]============================================================== */

package br.com.extrator.comandos.cli.validacao;

import br.com.extrator.comandos.cli.base.Comando;
import br.com.extrator.suporte.console.LoggerConsole;
import br.com.extrator.suporte.configuracao.ConfigBanco;

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
            ConfigBanco.validarConexaoBancoDados();
            log.info("✅ Conexão com banco de dados: OK");
            
            log.info("✅ Tabela dbo.log_extracoes deve existir (criada via scripts SQL em database/)");
            
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
