package br.com.extrator.comandos;

/**
 * Comando responsável por exibir a ajuda do sistema.
 */
public class ExibirAjudaComando implements Comando {
    
    @Override
    public void executar(String[] args) throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("🚀 SISTEMA DE EXTRAÇÃO DE DADOS - ESL CLOUD");
        System.out.println("=".repeat(80));
        System.out.println();
        System.out.println("COMANDOS DISPONÍVEIS:");
        System.out.println();
        System.out.println("  (sem argumentos)     Executa extração completa de todas as APIs");
        System.out.println("  --validar            Valida configurações e conectividade");
        System.out.println("  --introspeccao       Realiza introspecção do schema GraphQL");
        System.out.println("  --auditoria          Executa auditoria dos dados (últimas 24h)");
        System.out.println("  --auditoria --periodo YYYY-MM-DD YYYY-MM-DD");
        System.out.println("                       Executa auditoria para período específico");
        System.out.println("  --testar-api [tipo]  Testa API específica:");
        System.out.println("                         rest      - Testa API REST");
        System.out.println("                         graphql   - Testa API GraphQL");
        System.out.println("                         dataexport- Testa API DataExport");
        System.out.println("  --ajuda, --help      Exibe esta mensagem de ajuda");
        System.out.println();
        System.out.println("EXEMPLOS:");
        System.out.println("  java -jar extrator.jar");
        System.out.println("  java -jar extrator.jar --validar");
        System.out.println("  java -jar extrator.jar --auditoria");
        System.out.println("  java -jar extrator.jar --auditoria --periodo 2024-01-01 2024-01-31");
        System.out.println("  java -jar extrator.jar --testar-api rest");
        System.out.println();
        System.out.println("=".repeat(80));
    }
}