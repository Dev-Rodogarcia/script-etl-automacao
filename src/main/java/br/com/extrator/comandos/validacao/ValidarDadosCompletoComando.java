package br.com.extrator.comandos.validacao;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import br.com.extrator.comandos.base.Comando;
import br.com.extrator.util.banco.GerenciadorConexao;
import br.com.extrator.util.console.LoggerConsole;
import br.com.extrator.util.console.BannerUtil;

/**
 * Comando para validação completa dos dados extraídos.
 * Executa todos os scripts SQL de validação e gera um relatório consolidado.
 * 
 * Segue a "Prova dos 9" do engenheiro de dados - a paranoia saudável que separa
 * um júnior de um sênior. Se você não tiver essa dúvida, algo está errado.
 * 
 * Validações executadas:
 * 1. Validação de Completude (API vs Banco)
 * 2. Validação de Gaps (Sequências)
 * 3. Validação de Integridade (Chaves, Duplicados)
 * 4. Validação de Qualidade (NULLs, Valores Esperados)
 * 5. Validação de Metadata (Backup JSON)
 * 
 * @author Lucas Andrade (@valentelucass) - lucasmac.dev@gmail.com
 * 
 * 💡 "Trust, but verify" - Ronald Reagan (e todo engenheiro de dados que se preze)
 */
public class ValidarDadosCompletoComando implements Comando {
    private static final LoggerConsole log = LoggerConsole.getLogger(ValidarDadosCompletoComando.class);
    
    private static final String[] SCRIPTS_VALIDACAO = {
        "validacao/validar_completude_todas_entidades.sql",
        "validacao/validar_gaps_sequencias.sql",
        "validacao/validar_integridade_chaves.sql",
        "validacao/validar_qualidade_dados.sql",
        "validacao/validar_metadata_backup.sql"
    };
    
    @Override
    public void executar(final String[] args) throws Exception {
        // Exibir banner especial de validação (se existir)
        try {
            BannerUtil.exibirBanner("banners/banner-validacao.txt");
        } catch (final Exception e) {
            // Se não existir, usar banner padrão
            log.console("╔══════════════════════════════════════════════════════════════════════════════╗");
            log.console("║                                                                              ║");
            log.info("   ║           🔍 VALIDAÇÃO COMPLETA DOS DADOS - PROVA DOS 9                      ║");
            log.console("║                                                                              ║");
            log.console("╚══════════════════════════════════════════════════════════════════════════════╝");
        }
        log.console("");
        log.info("Este comando executa validações profundas para garantir integridade dos dados.");
        log.info("Baseado na 'Prova dos 9' - a paranoia saudável de todo engenheiro de dados.");
        log.console("");
        
        try (Connection conn = GerenciadorConexao.obterConexao()) {
            log.info("✅ Conexão com banco de dados estabelecida");
            log.console("");
            
            int scriptsExecutados = 0;
            int scriptsComErro = 0;
            final List<String> erros = new ArrayList<>();
            
            for (final String scriptName : SCRIPTS_VALIDACAO) {
                try {
                    log.console("═══════════════════════════════════════════════════════════════════════════════");
                    log.info("📄 Executando: {}", scriptName);
                    log.console("═══════════════════════════════════════════════════════════════════════════════");
                    log.console("");
                    
                    final String sql = carregarScriptSQL(scriptName);
                    executarScriptSQL(conn, sql, scriptName);
                    
                    scriptsExecutados++;
                    log.console("");
                    log.info("✅ {} executado com sucesso", scriptName);
                    log.console("");
                    
                } catch (final Exception e) {
                    scriptsComErro++;
                    final String erro = String.format("❌ Erro ao executar %s: %s", scriptName, e.getMessage());
                    log.error(erro, e);
                    erros.add(erro);
                    log.console("");
                }
            }
            
            // Resumo final
            log.console("╔══════════════════════════════════════════════════════════════════════════════╗");
            log.console("║                        📊 RESUMO DA VALIDAÇÃO                                ║");
            log.console("╚══════════════════════════════════════════════════════════════════════════════╝");
            log.console("");
            log.info("Scripts executados: {}/{}", scriptsExecutados, SCRIPTS_VALIDACAO.length);
            
            if (scriptsComErro > 0) {
                log.error("Scripts com erro: {}", scriptsComErro);
                log.console("");
                log.info("Erros encontrados:");
                for (final String erro : erros) {
                    log.error("  {}", erro);
                }
            } else {
                log.info("✅ Todos os scripts foram executados com sucesso!");
            }
            
            log.console("");
            log.console("═══════════════════════════════════════════════════════════════════════════════");
            log.info("💡 INTERPRETAÇÃO DOS RESULTADOS:");
            log.console("═══════════════════════════════════════════════════════════════════════════════");
            log.console("");
            log.info("1. COMPLETUDE: Números da API devem coincidir com o Banco");
            log.info("2. GAPS: Pequenos gaps são normais, gaps grandes podem indicar falhas");
            log.info("3. INTEGRIDADE: Não deve haver duplicados em chaves primárias");
            log.info("4. QUALIDADE: Campos críticos não devem estar NULL");
            log.info("5. METADATA: Deve estar 100% preenchido (backup JSON)");
            log.console("");
            log.info("✅ Se todas as validações passaram, seus dados estão sólidos!");
            log.console("");
            log.console("💡 Sistema desenvolvido por @valentelucass (lucasmac.dev@gmail.com)");
            log.console("   'Dados não mentem, mas você precisa saber como perguntar'");
            log.console("");
            
        } catch (final SQLException e) {
            log.error("❌ Erro ao conectar ao banco de dados: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Carrega um script SQL do diretório resources/sql
     */
    private String carregarScriptSQL(final String scriptName) throws Exception {
        final String resourcePath = "/sql/" + scriptName;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new Exception("Script SQL não encontrado: " + resourcePath);
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines()
                    .collect(Collectors.joining("\n"));
            }
        }
    }
    
    /**
     * Executa um script SQL e exibe os resultados formatados
     * Remove comandos PRINT e GO (específicos do SQL Server Management Studio)
     */
    private void executarScriptSQL(final Connection conn, final String sql, final String scriptName) 
            throws SQLException {
        // Remover comandos PRINT e GO (não suportados pelo JDBC)
        String sqlProcessado = sql;
        
        // Remover linhas com PRINT (mantém apenas SELECT e outros comandos SQL válidos)
        sqlProcessado = sqlProcessado.replaceAll("(?i)^\\s*PRINT\\s+.*$", "");
        
        // Dividir por GO (se houver) e executar cada batch
        final String[] batches = sqlProcessado.split("(?i)^\\s*GO\\s*$", -1);
        
        for (final String batch : batches) {
            final String batchTrimmed = batch.trim();
            if (batchTrimmed.isEmpty()) {
                continue;
            }
            
            try (Statement stmt = conn.createStatement()) {
                // Executar o batch
                final boolean hasResultSet = stmt.execute(batchTrimmed);
                
                // Processar todos os resultados (pode haver múltiplos ResultSets)
                do {
                    if (hasResultSet) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            exibirResultado(rs);
                        }
                    }
                } while (stmt.getMoreResults() || stmt.getUpdateCount() != -1);
            } catch (final SQLException e) {
                // Log do erro mas continua com os próximos batches
                log.warn("Erro ao executar batch do script {}: {}", scriptName, e.getMessage());
                // Re-throw apenas se for erro crítico (não relacionado a PRINT/GO)
                if (!e.getMessage().toLowerCase().contains("print") 
                    && !e.getMessage().toLowerCase().contains("incorrect syntax")) {
                    throw e;
                }
            }
        }
    }
    
    /**
     * Exibe o resultado de um ResultSet de forma formatada
     */
    private void exibirResultado(final ResultSet rs) throws SQLException {
        final ResultSetMetaData metaData = rs.getMetaData();
        final int columnCount = metaData.getColumnCount();
        
        if (columnCount == 0) {
            return;
        }
        
        // Coletar todos os dados primeiro
        final List<List<String>> rows = new ArrayList<>();
        final List<Integer> columnWidths = new ArrayList<>();
        
        // Inicializar larguras das colunas
        for (int i = 1; i <= columnCount; i++) {
            columnWidths.add(metaData.getColumnName(i).length());
        }
        
        // Coletar dados e calcular larguras
        while (rs.next()) {
            final List<String> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                final Object value = rs.getObject(i);
                final String strValue = value != null ? value.toString() : "NULL";
                row.add(strValue);
                // Atualizar largura se necessário
                if (strValue.length() > columnWidths.get(i - 1)) {
                    columnWidths.set(i - 1, Math.min(strValue.length(), 100)); // Limitar a 100 chars
                }
            }
            rows.add(row);
        }
        
        // Se não há dados, não exibir nada
        if (rows.isEmpty()) {
            log.console("(Nenhum resultado)");
            return;
        }
        
        // Exibir cabeçalhos
        final StringBuilder header = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                header.append(" | ");
            }
            final String colName = metaData.getColumnName(i);
            header.append(String.format("%-" + columnWidths.get(i - 1) + "s", colName));
        }
        log.console(header.toString());
        
        // Linha separadora
        final StringBuilder separator = new StringBuilder();
        for (int i = 0; i < columnCount; i++) {
            if (i > 0) {
                separator.append("-+-");
            }
            separator.append("-".repeat(columnWidths.get(i)));
        }
        log.console(separator.toString());
        
        // Exibir dados
        for (final List<String> row : rows) {
            final StringBuilder rowStr = new StringBuilder();
            for (int i = 0; i < columnCount; i++) {
                if (i > 0) {
                    rowStr.append(" | ");
                }
                String value = row.get(i);
                // Truncar se muito longo
                if (value.length() > columnWidths.get(i)) {
                    value = value.substring(0, columnWidths.get(i) - 3) + "...";
                }
                rowStr.append(String.format("%-" + columnWidths.get(i) + "s", value));
            }
            log.console(rowStr.toString());
        }
        
        log.console("");
        log.info("Total: {} linha(s)", rows.size());
    }
}
