package br.com.extrator.comandos;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.extrator.util.GerenciadorConexao;

/**
 * Comando para validar contagem de manifestos extraídos vs salvos
 */
public class ValidarManifestosComando implements Comando {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidarManifestosComando.class);
    
    @Override
    public void executar(final String[] args) throws Exception {
        System.out.println("===============================================================================");
        System.out.println("                    VALIDAÇÃO DE MANIFESTOS");
        System.out.println("===============================================================================");
        System.out.println();
        
        try (Connection conn = GerenciadorConexao.obterConexao()) {
            validarManifestos(conn);
            
            // Executar SQLs adicionais de validação
            System.out.println();
            System.out.println("===============================================================================");
            System.out.println("                    EXECUTANDO SQLs DE VALIDAÇÃO");
            System.out.println("===============================================================================");
            System.out.println();
            
            executarSqlsValidacao(conn);
            
        } catch (final SQLException e) {
            logger.error("❌ Erro ao validar manifestos: {}", e.getMessage(), e);
            System.err.println("❌ Erro ao conectar ao banco de dados: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Executa os SQLs de validação equivalentes aos arquivos em resources/sql
     */
    private void executarSqlsValidacao(final Connection conn) throws SQLException {
        // 1. Identificar duplicados falsos
        System.out.println("📄 IDENTIFICAR DUPLICADOS FALSOS:");
        System.out.println("(Manifestos com mesmo sequence_code mas identificador_unico diferente)");
        System.out.println();
        
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                """
                   SELECT
                     sequence_code,
                     COUNT(*) as total_registros,
                     COUNT(DISTINCT identificador_unico) as identificadores_unicos,
                     MIN(data_extracao) as primeira_extracao,
                     MAX(data_extracao) as ultima_extracao
                   FROM manifestos
                   WHERE pick_sequence_code IS NULL
                   GROUP BY sequence_code
                   HAVING COUNT(*) > 1 AND COUNT(DISTINCT identificador_unico) > 1
                   ORDER BY COUNT(*) DESC""")) {
            exibirResultado(rs);
        }
        
        System.out.println();
        System.out.println("===============================================================================");
        System.out.println();
        
        // 2. Validação da correção do identificador único
        System.out.println("📄 VALIDAÇÃO DA CORREÇÃO DO IDENTIFICADOR ÚNICO:");
        System.out.println();
        
        // Teste 1: Duplicados falsos
        System.out.println("TESTE 1: Verificar duplicados falsos");
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                """
                   SELECT
                     sequence_code,
                     COUNT(*) as total
                   FROM manifestos
                   WHERE pick_sequence_code IS NULL
                   GROUP BY sequence_code
                   HAVING COUNT(*) > 1""")) {
            exibirResultado(rs);
        }
        System.out.println();
        
        // Teste 2: Identificadores NULL
        System.out.println("TESTE 2: Verificar identificadores NULL");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT COUNT(*) as total_com_identificador_null FROM manifestos WHERE identificador_unico IS NULL")) {
            exibirResultado(rs);
        }
        System.out.println();
        
        // Teste 3: Distribuição de pick_sequence_code
        System.out.println("TESTE 3: Distribuição de pick_sequence_code");
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                """
                   SELECT
                     CASE
                       WHEN pick_sequence_code IS NOT NULL THEN 'Com pick_sequence_code'
                       ELSE 'Sem pick_sequence_code (usa hash)'
                     END as tipo,
                     COUNT(*) as total,
                     CAST(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM manifestos) AS DECIMAL(5,2)) as percentual
                   FROM manifestos
                   GROUP BY
                     CASE
                       WHEN pick_sequence_code IS NOT NULL THEN 'Com pick_sequence_code'
                       ELSE 'Sem pick_sequence_code (usa hash)'
                     END""")) {
            exibirResultado(rs);
        }
        System.out.println();
        
        // Teste 4: Integridade de chave composta
        System.out.println("TESTE 4: Integridade de chave composta");
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                """
                   SELECT
                     sequence_code,
                     identificador_unico,
                     COUNT(*) as total
                   FROM manifestos
                   GROUP BY sequence_code, identificador_unico
                   HAVING COUNT(*) > 1""")) {
            exibirResultado(rs);
        }
        System.out.println();
        
        // Teste 5: Resumo final
        System.out.println("TESTE 5: Resumo final");
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                """
                   SELECT
                     'Total de manifestos' as metrica,
                     COUNT(*) as valor
                   FROM manifestos
                   UNION ALL
                   SELECT
                     'Com pick_sequence_code' as metrica,
                     COUNT(*) as valor
                   FROM manifestos
                   WHERE pick_sequence_code IS NOT NULL
                   UNION ALL
                   SELECT
                     'Sem pick_sequence_code (usa hash)' as metrica,
                     COUNT(*) as valor
                   FROM manifestos
                   WHERE pick_sequence_code IS NULL
                   UNION ALL
                   SELECT
                     'Com identificador_unico NULL' as metrica,
                     COUNT(*) as valor
                   FROM manifestos
                   WHERE identificador_unico IS NULL
                   UNION ALL
                   SELECT
                     'Duplicados falsos (mesmo sequence_code, sem pick)' as metrica,
                     COUNT(*) as valor
                   FROM (
                     SELECT sequence_code
                     FROM manifestos
                     WHERE pick_sequence_code IS NULL
                     GROUP BY sequence_code
                     HAVING COUNT(*) > 1
                   ) as duplicados""")) {
            exibirResultado(rs);
        }
        System.out.println();
    }
    
    
    /**
     * Exibe o resultado de um ResultSet de forma formatada
     */
    private void exibirResultado(final ResultSet rs) throws SQLException {
        final java.sql.ResultSetMetaData metaData = rs.getMetaData();
        final int columnCount = metaData.getColumnCount();
        
        // Exibir cabeçalhos
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                System.out.print(" | ");
            }
            System.out.print(metaData.getColumnName(i));
        }
        System.out.println();
        
        // Linha separadora
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                System.out.print("-+-");
            }
            for (int j = 0; j < metaData.getColumnName(i).length(); j++) {
                System.out.print("-");
            }
        }
        System.out.println();
        
        // Exibir dados
        int rowCount = 0;
        while (rs.next()) {
            rowCount++;
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) {
                    System.out.print(" | ");
                }
                final Object value = rs.getObject(i);
                System.out.print(value != null ? value.toString() : "NULL");
            }
            System.out.println();
        }
        
        if (rowCount == 0) {
            System.out.println("(0 linhas)");
        } else {
            System.out.println();
            System.out.println("Total: " + rowCount + " linha(s)");
        }
    }
    
    private void validarManifestos(final Connection conn) throws SQLException {
        // 1. Última extração
        System.out.println("📋 ÚLTIMA EXTRAÇÃO:");
        System.out.println();
        
        Integer registrosExtraidos = null;
        String timestampFim = null;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 """
                    SELECT TOP 1 registros_extraidos, status_final, \
                    CONVERT(VARCHAR, timestamp_fim, 120) as timestamp_fim, \
                    mensagem \
                    FROM log_extracoes \
                    WHERE entidade = 'manifestos' \
                    ORDER BY timestamp_fim DESC""")) {
            if (rs.next()) {
                registrosExtraidos = rs.getInt("registros_extraidos");
                final String statusFinal = rs.getString("status_final");
                timestampFim = rs.getString("timestamp_fim");
                final String mensagem = rs.getString("mensagem");
                System.out.println("Data/Hora fim: " + timestampFim);
                System.out.println("Registros extraídos (API): " + registrosExtraidos);
                System.out.println("Status: " + statusFinal);
                if (mensagem != null && !mensagem.trim().isEmpty()) {
                    System.out.println("Mensagem: " + mensagem);
                }
            } else {
                System.out.println("⚠️ Nenhuma extração de manifestos encontrada no log_extracoes.");
            }
        }
        System.out.println();
        
        // 2. Contagem no banco
        System.out.println("📊 CONTAGEM NO BANCO:");
        System.out.println();
        
        int totalBanco = 0;
        int totalUltimas24h = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM manifestos")) {
            if (rs.next()) {
                totalBanco = rs.getInt("total");
            }
        }
        
        // Contar registros das últimas 24 horas
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT COUNT(*) as total FROM manifestos " +
                 "WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())")) {
            if (rs.next()) {
                totalUltimas24h = rs.getInt("total");
            }
        }
        
        // Contar registros desde a última extração (mais preciso)
        int totalDesdeUltimaExtracao = 0;
        if (timestampFim != null) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) as total FROM manifestos " +
                     "WHERE data_extracao >= CAST((SELECT TOP 1 timestamp_fim FROM log_extracoes WHERE entidade = 'manifestos' ORDER BY timestamp_fim DESC) AS DATETIME2)")) {
                if (rs.next()) {
                    totalDesdeUltimaExtracao = rs.getInt("total");
                }
            } catch (final SQLException e) {
                // Se houver erro (ex: tipos incompatíveis), usar fallback
                logger.warn("Erro ao contar registros desde última extração: {}", e.getMessage());
            }
        }
        
        System.out.println("Total de registros na tabela (todos): " + totalBanco);
        System.out.println("Total de registros (últimas 24h): " + totalUltimas24h);
        if (timestampFim != null) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) as total FROM manifestos " +
                     "WHERE data_extracao >= CAST((SELECT TOP 1 timestamp_fim FROM log_extracoes WHERE entidade = 'manifestos' ORDER BY timestamp_fim DESC) AS DATETIME2)")) {
                if (rs.next()) {
                    totalDesdeUltimaExtracao = rs.getInt("total");
                    System.out.println("Total de registros (desde última extração): " + totalDesdeUltimaExtracao);
                }
            } catch (final SQLException e) {
                // Se houver erro, logar mas não interromper validação
                logger.warn("Erro ao contar registros desde última extração: {}", e.getMessage());
            }
        }
        System.out.println();
        
        // 3. Comparação
        System.out.println("🔍 COMPARAÇÃO:");
        System.out.println();
        
        if (registrosExtraidos != null) {
            System.out.println("Registros no log_extracoes (última execução): " + registrosExtraidos);
            System.out.println("Registros no banco (últimas 24h): " + totalUltimas24h);
            if (timestampFim != null) {
                System.out.println("Registros no banco (desde última extração): " + totalDesdeUltimaExtracao);
            }
            System.out.println("Registros no banco (total): " + totalBanco);
            System.out.println();
            System.out.println("💡 NOTA: O valor em 'log_extracoes' é da última execução registrada.");
            System.out.println("   - Se for de ANTES da deduplicação, pode incluir duplicados da API");
            System.out.println("   - Após deduplicação, esse valor deve coincidir com os registros no banco");
            System.out.println("   - Execute uma nova extração para ver os valores atualizados");
            System.out.println();
            
            // Comparar com registros desde a última extração (mais preciso) ou últimas 24h (fallback)
            // Usar "desde última extração" se timestampFim estiver disponível (mesmo que totalDesdeUltimaExtracao seja 0)
            int diferenca;
            String tipoComparacao;
            if (timestampFim != null) {
                // Usar totalDesdeUltimaExtracao se disponível (query executou com sucesso)
                // Se totalDesdeUltimaExtracao ainda for 0, pode ser que a query não funcionou ou realmente não há registros
                // Nesse caso, usar totalUltimas24h como fallback
                if (totalDesdeUltimaExtracao >= 0) {
                    diferenca = registrosExtraidos - totalDesdeUltimaExtracao;
                    tipoComparacao = "desde última extração";
                } else {
                    // Se query falhou, usar fallback
                    diferenca = registrosExtraidos - totalUltimas24h;
                    tipoComparacao = "últimas 24h";
                }
            } else {
                diferenca = registrosExtraidos - totalUltimas24h;
                tipoComparacao = "últimas 24h";
            }
            
            if (diferenca == 0) {
                System.out.println("✅ OK - Números coincidem (" + tipoComparacao + ")!");
                System.out.println("   O valor do log_extracoes corresponde aos registros no banco.");
                System.out.println("   Isso indica que a extração funcionou corretamente.");
            } else if (diferenca > 0) {
                System.out.println("⚠️ DIFERENÇA: " + diferenca + " registros a mais no log que no banco (" + tipoComparacao + ")");
                System.out.println("   Valor no log_extracoes: " + registrosExtraidos);
                if (timestampFim != null) {
                    System.out.println("   Encontrado no banco (desde última extração): " + totalDesdeUltimaExtracao);
                }
                System.out.println("   Encontrado no banco (últimas 24h): " + totalUltimas24h);
                System.out.println();
                System.out.println("💡 Interpretação:");
                System.out.println("   - Se o log é ANTIGO (antes da deduplicação): NORMAL - duplicados foram removidos");
                System.out.println("   - Se o log é RECENTE: pode indicar UPDATEs (registros atualizados, não inseridos)");
                System.out.println("   - UPDATEs não adicionam linhas, então há menos linhas no banco");
                System.out.println("   - Isso é ESPERADO quando script roda periodicamente (1h buscando últimas 24h)");
                System.out.println();
                System.out.println("🔍 Se diferença for muito grande, verificar:");
                System.out.println("   - Erro durante salvamento (verifique logs)");
                System.out.println("   - MERGE retornou rowsAffected > 0 mas registro não foi salvo");
                System.out.println("   - Alguns registros falharam na validação silenciosamente");
                System.out.println("   - Problema com chave composta (identificador_unico)");
                System.out.println();
                System.out.println("💡 RECOMENDAÇÃO: Execute uma nova extração para gerar log atualizado com deduplicação.");
            } else {
                System.out.println("⚠️ ATENÇÃO - Há " + Math.abs(diferenca) + " registros a mais no banco!");
                System.out.println("   Valor no log_extracoes: " + registrosExtraidos);
                if (timestampFim != null) {
                    System.out.println("   Encontrado no banco (desde última extração): " + totalDesdeUltimaExtracao);
                }
                System.out.println("   Encontrado no banco (últimas 24h): " + totalUltimas24h);
                System.out.println();
                System.out.println("💡 Possíveis causas:");
                System.out.println("   - Execuções anteriores adicionaram registros");
                System.out.println("   - Duplicados naturais estão sendo preservados (correto para manifestos!)");
                System.out.println("   - Dados de períodos anteriores ainda no banco");
                System.out.println("   - Registros adicionados manualmente ou por outros processos");
                System.out.println("   - O log_extracoes pode estar desatualizado");
            }
        } else {
            System.out.println("⚠️ Não foi possível comparar - nenhuma extração encontrada no log.");
            System.out.println("   Total no banco (últimas 24h): " + totalUltimas24h);
            System.out.println("   Total no banco (todos): " + totalBanco);
        }
        System.out.println();
        
        // 4. Duplicados por sequence_code
        System.out.println("🔍 DUPLICADOS (por sequence_code):");
        System.out.println();
        
        int duplicadosCount = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 """
                    SELECT sequence_code, COUNT(*) as quantidade \
                    FROM manifestos \
                    GROUP BY sequence_code \
                    HAVING COUNT(*) > 1 \
                    ORDER BY quantidade DESC""")) {
            boolean encontrou = false;
            while (rs.next()) {
                if (!encontrou) {
                    System.out.println("Duplicados encontrados:");
                    encontrou = true;
                }
                final long sequenceCode = rs.getLong("sequence_code");
                final int quantidade = rs.getInt("quantidade");
                System.out.println("  sequence_code: " + sequenceCode + " - " + quantidade + " registros");
                duplicadosCount++;
            }
            if (!encontrou) {
                System.out.println("✅ Nenhum duplicado encontrado por sequence_code.");
            } else {
                System.out.println();
                System.out.println("⚠️ Total de sequence_codes com duplicados: " + duplicadosCount);
            }
        }
        System.out.println();
        
        // 5. Verificação de problemas com identificador_unico
        System.out.println("🔍 VERIFICAÇÃO DE ESTRUTURA (identificador_unico):");
        System.out.println();
        
        boolean temIdentificadorUnico = false;
        int registrosSemIdentificador = 0;
        int identificadoresInvalidos = 0;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 """
                    SELECT COUNT(*) as existe \
                    FROM INFORMATION_SCHEMA.COLUMNS \
                    WHERE TABLE_NAME = 'manifestos' AND COLUMN_NAME = 'identificador_unico'""")) {
            if (rs.next() && rs.getInt("existe") > 0) {
                temIdentificadorUnico = true;
                System.out.println("✅ Coluna identificador_unico existe.");
                
                // Verificar registros sem identificador_unico
                try (Statement stmt2 = conn.createStatement();
                     ResultSet rs2 = stmt2.executeQuery(
                         "SELECT COUNT(*) as total FROM manifestos WHERE identificador_unico IS NULL")) {
                    if (rs2.next()) {
                        registrosSemIdentificador = rs2.getInt("total");
                    }
                }
                
                // Verificar identificadores muito longos (se houver problema)
                try (Statement stmt3 = conn.createStatement();
                     ResultSet rs3 = stmt3.executeQuery(
                         "SELECT COUNT(*) as total FROM manifestos WHERE LEN(identificador_unico) > 100")) {
                    if (rs3.next()) {
                        identificadoresInvalidos = rs3.getInt("total");
                    }
                }
                
                if (registrosSemIdentificador > 0) {
                    System.out.println("❌ PROBLEMA: " + registrosSemIdentificador + " registros com identificador_unico NULL!");
                } else {
                    System.out.println("✅ Todos os registros têm identificador_unico.");
                }
                
                if (identificadoresInvalidos > 0) {
                    System.out.println("❌ PROBLEMA: " + identificadoresInvalidos + " registros com identificador_unico muito longo (>100 chars)!");
                }
                
                // Verificar duplicados na chave composta
                try (Statement stmt4 = conn.createStatement();
                     ResultSet rs4 = stmt4.executeQuery(
                         """
                            SELECT sequence_code, identificador_unico, COUNT(*) as quantidade \
                            FROM manifestos \
                            GROUP BY sequence_code, identificador_unico \
                            HAVING COUNT(*) > 1""")) {
                    int duplicadosChaveComposta = 0;
                    while (rs4.next()) {
                        duplicadosChaveComposta++;
                    }
                    if (duplicadosChaveComposta == 0) {
                        System.out.println("✅ Nenhum duplicado na chave composta (correto - MERGE está funcionando).");
                    } else {
                        System.out.println("❌ PROBLEMA: " + duplicadosChaveComposta + " duplicados na chave composta!");
                        System.out.println("   Isso não deveria acontecer com a constraint UNIQUE.");
                    }
                }
            } else {
                System.out.println("ℹ️ Coluna identificador_unico não existe ainda (tabela não migrada).");
                System.out.println("   Isso significa que a tabela ainda usa a estrutura antiga.");
            }
        }
        System.out.println();
        
        // 6. Análise de registros com pick_sequence_code NULL
        System.out.println("🔍 ANÁLISE DE pick_sequence_code:");
        System.out.println();
        
        int registrosComPickNull = 0;
        int registrosComPickNotNull = 0;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 """
                    SELECT \
                      SUM(CASE WHEN pick_sequence_code IS NULL THEN 1 ELSE 0 END) as com_null, \
                      SUM(CASE WHEN pick_sequence_code IS NOT NULL THEN 1 ELSE 0 END) as com_valor \
                    FROM manifestos""")) {
            if (rs.next()) {
                registrosComPickNull = rs.getInt("com_null");
                registrosComPickNotNull = rs.getInt("com_valor");
                System.out.println("Registros com pick_sequence_code NULL: " + registrosComPickNull);
                System.out.println("Registros com pick_sequence_code não NULL: " + registrosComPickNotNull);
                if (temIdentificadorUnico) {
                    System.out.println();
                    System.out.println("💡 Registros com pick_sequence_code NULL usam hash do metadata como identificador_unico.");
                    System.out.println("   Registros com pick_sequence_code não NULL usam o valor como identificador_unico.");
                }
            }
        }
        System.out.println();
        
        // 7. Análise detalhada da diferença
        if (registrosExtraidos != null && registrosExtraidos > totalUltimas24h) {
            System.out.println("🔍 ANÁLISE DETALHADA DA DIFERENÇA:");
            System.out.println();
            System.out.println("Faltam " + (registrosExtraidos - totalUltimas24h) + " registros.");
            System.out.println();
            
            // Verificar se há registros com data_extracao muito antiga (possível problema de timezone)
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     """
                        SELECT \
                          MIN(data_extracao) as data_minima, \
                          MAX(data_extracao) as data_maxima, \
                          COUNT(*) as total \
                        FROM manifestos \
                        WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE())""")) {
                if (rs.next()) {
                    final String dataMinima = rs.getString("data_minima");
                    final String dataMaxima = rs.getString("data_maxima");
                    final int total = rs.getInt("total");
                    System.out.println("📅 Análise de data_extracao (últimas 24h):");
                    System.out.println("   Data mínima: " + (dataMinima != null ? dataMinima : "N/A"));
                    System.out.println("   Data máxima: " + (dataMaxima != null ? dataMaxima : "N/A"));
                    System.out.println("   Total: " + total);
                    System.out.println();
                }
            } catch (final SQLException e) {
                logger.warn("Erro ao analisar data_extracao: {}", e.getMessage());
            }
            
            // Verificar se há registros duplicados exatamente iguais (mesma chave composta)
            if (temIdentificadorUnico) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                         """
                            SELECT sequence_code, identificador_unico, COUNT(*) as quantidade \
                            FROM manifestos \
                            WHERE data_extracao >= DATEADD(HOUR, -24, GETDATE()) \
                            GROUP BY sequence_code, identificador_unico \
                            HAVING COUNT(*) > 1""")) {
                    int duplicadosExatos = 0;
                    while (rs.next()) {
                        duplicadosExatos++;
                    }
                    if (duplicadosExatos > 0) {
                        System.out.println("⚠️ ATENÇÃO: " + duplicadosExatos + " pares (sequence_code, identificador_unico) duplicados!");
                        System.out.println("   Isso não deveria acontecer com a constraint UNIQUE.");
                    }
                }
            }
            
            // Verificar se há registros com data_extracao NULL ou inválida
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     """
                        SELECT COUNT(*) as total \
                        FROM manifestos \
                        WHERE data_extracao IS NULL""")) {
                if (rs.next()) {
                    final int comDataNull = rs.getInt("total");
                    if (comDataNull > 0) {
                        System.out.println("⚠️ ATENÇÃO: " + comDataNull + " registros com data_extracao NULL!");
                    }
                }
            } catch (final SQLException e) {
                logger.warn("Erro ao verificar data_extracao NULL: {}", e.getMessage());
            }
            
            if (temIdentificadorUnico) {
                System.out.println();
                System.out.println("💡 Possíveis causas:");
                System.out.println("1. MERGE retornou rowsAffected > 0 mas registro não foi realmente salvo");
                System.out.println("2. Problema com commit/transação (registro foi revertido)");
                System.out.println("3. Registro foi inserido mas depois deletado por trigger/constraint");
                System.out.println("4. Problema de timezone (data_extracao em UTC vs hora local)");
                System.out.println("5. Um registro foi contado duas vezes no rowsAffected (MERGE pode retornar 2 em alguns casos)");
            } else {
                System.out.println();
                System.out.println("⚠️ ATENÇÃO: Tabela não migrada para chave composta!");
                System.out.println("   A tabela ainda usa sequence_code como PRIMARY KEY.");
                System.out.println("   Duplicados naturais podem estar sendo sobrescritos.");
                System.out.println();
                System.out.println("💡 Solução: Execute a migração para chave composta.");
            }
            System.out.println();
            System.out.println("📋 Ações recomendadas:");
            System.out.println("1. Verificar logs da última extração (especialmente erros silenciosos)");
            System.out.println("2. Verificar se há triggers na tabela manifestos que podem rejeitar registros");
            System.out.println("3. Verificar se há problemas de timezone entre data_extracao e comparação");
            System.out.println("4. Considerar adicionar logging mais detalhado no MERGE para identificar qual registro falhou");
        }
        System.out.println();
        
        // 8. Resumo final
        System.out.println("🎯 RESUMO FINAL:");
        System.out.println();
        System.out.println("Total no banco (últimas 24h): " + totalUltimas24h);
        if (timestampFim != null && totalDesdeUltimaExtracao > 0) {
            System.out.println("Total no banco (desde última extração): " + totalDesdeUltimaExtracao);
        }
        System.out.println("Total no banco (todos): " + totalBanco);
        if (registrosExtraidos != null) {
            System.out.println("Total extraído (API): " + registrosExtraidos);
            if (timestampFim != null && totalDesdeUltimaExtracao > 0) {
                System.out.println("Diferença (desde última extração): " + (registrosExtraidos - totalDesdeUltimaExtracao));
            }
            System.out.println("Diferença (últimas 24h): " + (registrosExtraidos - totalUltimas24h));
        }
        System.out.println("Duplicados por sequence_code: " + duplicadosCount);
        if (temIdentificadorUnico) {
            System.out.println("Registros sem identificador_unico: " + registrosSemIdentificador);
        }
        System.out.println("Registros com pick_sequence_code NULL: " + registrosComPickNull);
        System.out.println();
        System.out.println("===============================================================================");
        System.out.println();
        
        // 9. Identificar duplicados falsos (mesmo sequence_code, identificador_unico diferente)
        if (temIdentificadorUnico) {
            System.out.println("🔍 IDENTIFICAR DUPLICADOS FALSOS:");
            System.out.println();
            System.out.println("(Manifestos com mesmo sequence_code mas identificador_unico diferente)");
            System.out.println("(Isso indica que campos voláteis estavam no hash ANTES da correção)");
            System.out.println();
            
            int duplicadosFalsosCount = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     """
                        SELECT \
                          sequence_code, \
                          COUNT(*) as total_registros, \
                          COUNT(DISTINCT identificador_unico) as identificadores_unicos \
                        FROM manifestos \
                        WHERE pick_sequence_code IS NULL \
                        GROUP BY sequence_code \
                        HAVING COUNT(*) > 1 AND COUNT(DISTINCT identificador_unico) > 1 \
                        ORDER BY COUNT(*) DESC""")) {
                boolean encontrou = false;
                while (rs.next()) {
                    if (!encontrou) {
                        System.out.println("Duplicados falsos encontrados:");
                        encontrou = true;
                    }
                    final long sequenceCode = rs.getLong("sequence_code");
                    final int totalRegistros = rs.getInt("total_registros");
                    final int identificadoresUnicos = rs.getInt("identificadores_unicos");
                    System.out.println("  sequence_code: " + sequenceCode + 
                                     " - " + totalRegistros + " registros, " + 
                                     identificadoresUnicos + " identificadores diferentes");
                    duplicadosFalsosCount++;
                }
                if (!encontrou) {
                    System.out.println("✅ Nenhum duplicado falso encontrado!");
                    System.out.println("   Todos os manifestos com mesmo sequence_code têm mesmo identificador_unico.");
                    System.out.println("   Isso indica que a correção está funcionando corretamente.");
                } else {
                    System.out.println();
                    System.out.println("⚠️ Total de sequence_codes com duplicados falsos: " + duplicadosFalsosCount);
                    System.out.println();
                    System.out.println("💡 Interpretação:");
                    System.out.println("   - Esses são duplicados criados ANTES da correção do identificador único");
                    System.out.println("   - Eles têm mesmo sequence_code mas identificador_unico diferente");
                    System.out.println("   - Isso acontecia porque campos voláteis (mobile_read_at, etc.) estavam no hash");
                    System.out.println("   - Após a correção, novas extrações não criarão mais esses duplicados");
                    System.out.println();
                    System.out.println("💡 Solução:");
                    System.out.println("   - Execute uma nova extração completa");
                    System.out.println("   - Os duplicados falsos não serão mais criados");
                    System.out.println("   - Os existentes permanecerão no banco (são registros válidos)");
                }
            }
            System.out.println();
            System.out.println("===============================================================================");
            System.out.println();
            
            // 10. Validação da correção do identificador único
            System.out.println("✅ VALIDAÇÃO DA CORREÇÃO DO IDENTIFICADOR ÚNICO:");
            System.out.println();
            
            // Teste 1: Verificar se ainda há duplicados falsos
            System.out.println("TESTE 1: Verificar duplicados falsos");
            int duplicadosFalsosAposCorrecao = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     """
                        SELECT sequence_code \
                        FROM manifestos \
                        WHERE pick_sequence_code IS NULL \
                        GROUP BY sequence_code \
                        HAVING COUNT(*) > 1""")) {
                while (rs.next()) {
                    duplicadosFalsosAposCorrecao++;
                }
            }
            if (duplicadosFalsosAposCorrecao == 0) {
                System.out.println("  ✅ PASSOU: Nenhum duplicado falso (todos têm identificador_unico único)");
            } else {
                System.out.println("  ⚠️ ATENÇÃO: " + duplicadosFalsosAposCorrecao + " sequence_codes com múltiplos registros");
                System.out.println("     (Isso pode ser normal se são duplicados naturais com pick_sequence_code diferentes)");
            }
            System.out.println();
            
            // Teste 2: Verificar identificadores NULL
            System.out.println("TESTE 2: Verificar identificadores NULL");
            if (registrosSemIdentificador == 0) {
                System.out.println("  ✅ PASSOU: Todos os registros têm identificador_unico");
            } else {
                System.out.println("  ❌ FALHOU: " + registrosSemIdentificador + " registros sem identificador_unico");
            }
            System.out.println();
            
            // Teste 3: Verificar distribuição de pick_sequence_code
            System.out.println("TESTE 3: Distribuição de pick_sequence_code");
            System.out.println("  Registros com pick_sequence_code: " + registrosComPickNotNull + 
                             " (" + String.format("%.2f", (registrosComPickNotNull * 100.0 / totalBanco)) + "%)");
            System.out.println("  Registros sem pick_sequence_code (usa hash): " + registrosComPickNull + 
                             " (" + String.format("%.2f", (registrosComPickNull * 100.0 / totalBanco)) + "%)");
            System.out.println();
            
            // Teste 4: Verificar integridade de chave composta
            System.out.println("TESTE 4: Integridade de chave composta");
            int duplicadosChaveComposta = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     """
                        SELECT sequence_code, identificador_unico \
                        FROM manifestos \
                        GROUP BY sequence_code, identificador_unico \
                        HAVING COUNT(*) > 1""")) {
                while (rs.next()) {
                    duplicadosChaveComposta++;
                }
            }
            if (duplicadosChaveComposta == 0) {
                System.out.println("  ✅ PASSOU: Chave composta é única (sem duplicados)");
            } else {
                System.out.println("  ❌ FALHOU: " + duplicadosChaveComposta + " duplicados na chave composta");
                System.out.println("     (Isso não deveria acontecer - verifique constraint UNIQUE)");
            }
            System.out.println();
            
            // Teste 5: Resumo final
            System.out.println("TESTE 5: Resumo final");
            System.out.println("  Total de manifestos: " + totalBanco);
            System.out.println("  Com pick_sequence_code: " + registrosComPickNotNull);
            System.out.println("  Sem pick_sequence_code (usa hash): " + registrosComPickNull);
            System.out.println("  Com identificador_unico NULL: " + registrosSemIdentificador);
            System.out.println("  Duplicados falsos: " + duplicadosFalsosCount);
            System.out.println();
            
            // Conclusão
            boolean todosTestesPassaram = (registrosSemIdentificador == 0) && (duplicadosChaveComposta == 0);
            if (todosTestesPassaram) {
                System.out.println("✅ TODOS OS TESTES PASSARAM!");
                System.out.println("   A correção do identificador único está funcionando corretamente.");
            } else {
                System.out.println("⚠️ ALGUNS TESTES FALHARAM");
                System.out.println("   Revise os resultados acima para identificar problemas.");
            }
        } else {
            System.out.println("⚠️ VALIDAÇÃO DA CORREÇÃO NÃO PODE SER EXECUTADA");
            System.out.println("   A tabela não tem a coluna identificador_unico ainda.");
            System.out.println("   Execute a migração para chave composta primeiro.");
        }
        System.out.println();
        System.out.println("===============================================================================");
    }
}

