package br.com.extrator.comandos;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import br.com.extrator.runners.DataExportRunner;
import br.com.extrator.runners.GraphQLRunner;
import br.com.extrator.runners.RestRunner;

/**
 * Comando responsável por testar uma API específica do sistema.
 */
public class TestarApiComando implements Comando {
    
    @Override
    public void executar(String[] args) throws Exception {
        // Verifica se o tipo de API foi especificado
        if (args.length < 2) {
            System.err.println("❌ ERRO: Tipo de API não especificado.");
            System.err.println("Uso: --testar-api <tipo>");
            System.err.println("Tipos válidos: rest, graphql, dataexport");
            System.exit(1);
        }
        
        final String tipoApi = args[1];
        
        // Define data de hoje para buscar dados do dia atual
        final LocalDate dataHoje = LocalDate.now();
        
        System.out.println("🧪 Testando API: " + tipoApi.toUpperCase());
        System.out.println("Data de teste: " + dataHoje.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " (dados de hoje)");
        System.out.println("=".repeat(50));
        
        switch (tipoApi.toLowerCase()) {
            case "rest" -> RestRunner.executar(dataHoje);
            case "graphql" -> GraphQLRunner.executar(dataHoje);
            case "dataexport" -> DataExportRunner.executar(dataHoje);
            default -> {
                System.err.println("❌ ERRO: Tipo de API inválido: " + tipoApi);
                System.err.println("Tipos válidos: rest, graphql, dataexport");
                System.exit(1);
            }
        }
        
        System.out.println("✅ Teste da API " + tipoApi.toUpperCase() + " concluído com sucesso!");
    }
}