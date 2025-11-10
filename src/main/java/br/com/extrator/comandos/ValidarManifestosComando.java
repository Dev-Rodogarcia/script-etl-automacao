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
        } catch (final SQLException e) {
            logger.error("❌ Erro ao validar manifestos: {}", e.getMessage(), e);
            System.err.println("❌ Erro ao conectar ao banco de dados: " + e.getMessage());
            throw e;
        }
    }
    
    private void validarManifestos(final Connection conn) throws SQLException {
        // 1. Última extração
        System.out.println("📋 ÚLTIMA EXTRAÇÃO:");
        System.out.println();
        
        Integer registrosExtraidos = null;
        String statusFinal = null;
        String timestampFim = null;
        String mensagem = null;
        
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
                statusFinal = rs.getString("status_final");
                timestampFim = rs.getString("timestamp_fim");
                mensagem = rs.getString("mensagem");
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
    }
}

