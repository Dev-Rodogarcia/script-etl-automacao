package br.com.extrator.comandos.utilitarios;

import br.com.extrator.comandos.base.Comando;
import br.com.extrator.util.formatacao.ExportadorCSV;

/**
 * Comando responsável por exportar dados para CSV.
 * Permite exportar todas as tabelas ou uma tabela específica.
 */
public class ExportarCsvComando implements Comando {
    
    @Override
    public void executar(final String[] args) throws Exception {
        // Se houver argumento, é o nome da tabela específica
        final String tabelaEspecifica = (args != null && args.length > 1) ? args[1].trim() : null;
        
        // Chama o método main do ExportadorCSV diretamente
        if (tabelaEspecifica != null && !tabelaEspecifica.isEmpty()) {
            ExportadorCSV.main(new String[] { tabelaEspecifica });
        } else {
            ExportadorCSV.main(new String[0]);
        }
    }
}

