package br.com.extrator.util;

 
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Exportador de dados para CSV
 * Exporta todas as 8 entidades do banco para arquivos CSV
 */
public class ExportadorCSV {
    
    private static final String[] ENTIDADES = {
        "cotacoes",
        "coletas",
        "contas_a_pagar",
        "faturas_por_cliente",
        "fretes",
        "manifestos",
        "localizacao_cargas"
    };
    
    private static final String PASTA_DESTINO = "exports";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    public static void main(final String[] args) {
        System.out.println("===============================================================");
        System.out.println("  EXPORTADOR CSV - ESL CLOUD DATA");
        System.out.println("===============================================================");
        System.out.println();
        
        // Criar pasta de destino
        final Path pastaExports = Paths.get(PASTA_DESTINO);
        try {
            if (!Files.exists(pastaExports)) {
                Files.createDirectories(pastaExports);
                System.out.println("✅ Pasta criada: " + PASTA_DESTINO);
            }
        } catch (final IOException e) {
            System.err.println("❌ Erro ao criar pasta: " + e.getMessage());
            return;
        }
        
        final String timestamp = LocalDateTime.now().format(FORMATTER);
        int totalRegistros = 0;
        int entidadesExportadas = 0;
        
        System.out.println("📅 Timestamp: " + timestamp);
        System.out.println("📁 Destino: " + PASTA_DESTINO);
        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        
        // Exportar cada entidade
        for (int i = 0; i < ENTIDADES.length; i++) {
            final String entidade = ENTIDADES[i];
            System.out.printf("[%d/%d] 📊 Exportando: %s%n", i + 1, ENTIDADES.length, entidade);
            
            try {
                final int registros = exportarEntidade(entidade, timestamp);
                System.out.println("    ✅ Sucesso: " + registros + " registros");
                totalRegistros += registros;
                entidadesExportadas++;
            } catch (final Exception e) {
                System.err.println("    ❌ Erro: " + e.getMessage());
            }
            System.out.println();
        }
        
        // Resumo final
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  EXPORTAÇÃO CONCLUÍDA!");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("📊 Resumo:");
        System.out.println("   - Entidades exportadas: " + entidadesExportadas + "/" + ENTIDADES.length);
        System.out.println("   - Total de registros: " + totalRegistros);
        System.out.println("   - Pasta: " + Paths.get(PASTA_DESTINO).toAbsolutePath());
        System.out.println();
        System.out.println("💡 Próximos passos:");
        System.out.println("   1. Abra a pasta 'exports'");
        System.out.println("   2. Abra os CSVs no Excel/Google Sheets");
        System.out.println("   3. Compare com dados do portal");
        System.out.println();
    }
    
    /**
     * Exporta uma entidade para CSV
     */
    private static int exportarEntidade(final String entidade, final String timestamp) throws Exception {
        final String nomeArquivo = String.format("%s/%s_%s.csv", PASTA_DESTINO, entidade, timestamp);
        
        System.out.println("    🔍 Contando registros no banco...");
        
        // Primeiro, contar quantos registros existem no banco
        int totalNoBanco = 0;
        try (Connection connCount = GerenciadorConexao.obterConexao();
             Statement stmtCount = connCount.createStatement();
             ResultSet rsCount = stmtCount.executeQuery("SELECT COUNT(*) as total FROM " + entidade)) {
            if (rsCount.next()) {
                totalNoBanco = rsCount.getInt("total");
            }
        }
        
        System.out.println("    📊 Total de registros no banco: " + totalNoBanco);
        
        if (totalNoBanco == 0) {
            System.out.println("    ⚠️ Nenhum registro encontrado. CSV será criado apenas com cabeçalho.");
        }
        
        // Obter conexão do GerenciadorConexao
        try (Connection conn = GerenciadorConexao.obterConexao();
             Statement stmt = conn.createStatement();
             java.io.OutputStream os = new java.io.FileOutputStream(nomeArquivo);
             java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            os.write(new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF});
            
            // Configurar Statement para não limitar resultados
            // SQL Server pode ter limite padrão, então garantimos que não há limitação
            stmt.setFetchSize(0); // 0 = fetch all (padrão do driver)
            stmt.setMaxRows(0); // 0 = sem limite
            
            System.out.println("    🔄 Executando query SELECT * FROM " + entidade + "...");
            
            String origem;
            switch (entidade) {
                case "fretes" -> origem = "dbo.vw_fretes_powerbi";
                case "coletas" -> origem = "dbo.vw_coletas_powerbi";
                case "cotacoes" -> origem = "dbo.vw_cotacoes_powerbi";
                case "contas_a_pagar" -> origem = "dbo.vw_contas_a_pagar_powerbi";
                case "faturas_por_cliente" -> origem = "dbo.vw_faturas_por_cliente_powerbi";
                case "manifestos" -> origem = "dbo.vw_manifestos_powerbi";
                case "localizacao_cargas" -> origem = "dbo.vw_localizacao_cargas_powerbi";
                default -> origem = entidade;
            }
            final boolean usarViewPowerBI = !origem.equals(entidade);
            String query = "SELECT * FROM " + origem;
            // Ordenação consistente
            if (usarViewPowerBI) {
                // Evitar problemas com nomes com espaços usando posição
                query += " ORDER BY 1"; // ID
            } else {
                switch (entidade) {
                    case "manifestos", "cotacoes", "contas_a_pagar" -> query += " ORDER BY sequence_code";
                    case "localizacao_cargas" -> query += " ORDER BY sequence_number";
                    case "coletas", "fretes" -> query += " ORDER BY id";
                    case "faturas_por_cliente" -> query += " ORDER BY unique_id";
                    default -> {}
                }
            }
            
            try (ResultSet rs = stmt.executeQuery(query)) {
                final ResultSetMetaData metaData = rs.getMetaData();
                final int columnCount = metaData.getColumnCount();
                
                System.out.println("    📋 Colunas encontradas: " + columnCount);
                
                // Escrever cabeçalho com alias (label) quando existente
                for (int i = 1; i <= columnCount; i++) {
                    writer.append(metaData.getColumnLabel(i));
                    if (i < columnCount) {
                        writer.append(";");
                    }
                }
                writer.append("\r\n");
                
                // Escrever dados
                int count = 0;
                int logInterval = Math.max(100, totalNoBanco / 10); // Log a cada 10% ou 100 registros
                
                System.out.println("    📝 Escrevendo registros no CSV...");
                
                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        final Object value = rs.getObject(i);
                        if (value != null) {
                            String strValue = value.toString();
                            // Escapar valores que contenham vírgula ou aspas
                            if (strValue.contains(";") || strValue.contains("\"") || strValue.contains("\n")) {
                                strValue = "\"" + strValue.replace("\"", "\"\"") + "\"";
                            }
                            writer.append(strValue);
                        }
                        if (i < columnCount) {
                            writer.append(";");
                        }
                    }
                    writer.append("\r\n");
                    count++;
                    
                    // Log de progresso
                    if (count % logInterval == 0 || count == totalNoBanco) {
                        System.out.printf("    ⏳ Progresso: %d/%d registros (%.1f%%)\n", 
                                count, totalNoBanco, (count * 100.0 / totalNoBanco));
                    }
                }
                
                // Garantir que o buffer seja escrito
                writer.flush();
                
                System.out.println("    ✅ Exportação concluída: " + count + " registros escritos");
                
                // Verificar se há discrepância
                if (count != totalNoBanco) {
                    System.err.println("    ⚠️ ATENÇÃO: Discrepância detectada!");
                    System.err.println("       - Registros no banco: " + totalNoBanco);
                    System.err.println("       - Registros exportados: " + count);
                    System.err.println("       - Diferença: " + (totalNoBanco - count) + " registros");
                }
                
                return count;
            }
        }
    }
}
