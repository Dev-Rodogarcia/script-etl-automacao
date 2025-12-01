package br.com.extrator.comandos;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.runners.DataExportRunner;
import br.com.extrator.runners.GraphQLRunner;
import br.com.extrator.util.BannerUtil;

/**
 * Comando responsável por testar uma API específica do sistema.
 */
public class TestarApiComando implements Comando {
    private static final Logger logger = LoggerFactory.getLogger(TestarApiComando.class);
    
    @Override
    public void executar(final String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("❌ ERRO: Tipo de API não especificado.");
            System.err.println("Uso: --testar-api <tipo> [entidade]");
            System.err.println("Tipos válidos: graphql, dataexport");
            System.exit(1);
        }
        
        final String tipoApi = args[1];
        final String entidade = (args.length >= 3) ? args[2] : null;
        
        // Define data de hoje para buscar dados do dia atual
        final LocalDate dataHoje = LocalDate.now();
        
        // Exibe banner específico da API
        switch (tipoApi.toLowerCase()) {
            case "graphql" -> BannerUtil.exibirBannerApiGraphQL();
            case "dataexport" -> BannerUtil.exibirBannerApiDataExport();
            default -> {
                System.err.println("❌ ERRO: Tipo de API inválido: " + tipoApi);
                System.err.println("Tipos válidos: graphql, dataexport");
                System.exit(1);
            }
        }
        
        System.out.println("Data de teste: " + dataHoje.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " (dados de hoje)");
        System.out.println();
        
        try {
            switch (tipoApi.toLowerCase()) {
                case "graphql" -> {
                    if (entidade != null && !entidade.isBlank()) {
                        GraphQLRunner.executar(dataHoje, entidade);
                    } else {
                        GraphQLRunner.executar(dataHoje);
                    }
                }
                case "dataexport" -> {
                    if (entidade != null && !entidade.isBlank()) {
                        DataExportRunner.executar(dataHoje, entidade);
                    } else {
                        DataExportRunner.executar(dataHoje);
                    }
                }
                default -> {
                    System.err.println("❌ ERRO: Tipo de API inválido: " + tipoApi);
                    System.err.println("Tipos válidos: graphql, dataexport");
                    System.exit(1);
                }
            }
            
            // Exibe banner de sucesso
            BannerUtil.exibirBannerSucesso();
            System.out.println("✅ Teste da API " + tipoApi.toUpperCase() + " concluído com sucesso!");
            
        } catch (final Exception e) {
            BannerUtil.exibirBannerErro();
            System.err.println("❌ Erro durante execução: " + e.getMessage());
            logger.error("Erro durante execução: {}", e.getMessage(), e);
            throw e;
        }
    }
}
