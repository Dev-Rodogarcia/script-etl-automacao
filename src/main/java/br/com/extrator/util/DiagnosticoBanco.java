package br.com.extrator.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ferramenta de diagnóstico unificada para inspecionar o schema do banco de dados.
 * Esta classe deve ser usada apenas para desenvolvimento e depuração, executando seu método main.
 * Substitui as classes ListadorTabelasBanco e VerificadorTabelas.
 */
public class DiagnosticoBanco {

    public static void main(String[] args) {
        System.out.println("=== FERRAMENTA DE DIAGNÓSTICO DO BANCO DE DADOS ===");
        System.out.println();

        try (Connection connection = GerenciadorConexao.obterConexao()) {
            System.out.println("✅ Conectado ao banco de dados com sucesso!");
            System.out.println();

            listarTodasTabelas(connection);
            System.out.println();

            buscarTabelasPorTermos(connection);
            System.out.println();

            verificarTabelasCriticas(connection);

        } catch (SQLException e) {
            System.err.println("❌ Erro fatal durante o diagnóstico: " + e.getMessage());
        }
    }

    private static void listarTodasTabelas(Connection connection) throws SQLException {
        System.out.println("📋 LISTANDO TODAS AS TABELAS NO BANCO DE DADOS:");
        System.out.println("-".repeat(50));

        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
            List<String> nomeTabelas = new ArrayList<>();
            while (tables.next()) {
                String nomeTabela = tables.getString("TABLE_NAME");
                String esquema = tables.getString("TABLE_SCHEM");
                nomeTabelas.add(nomeTabela);
                System.out.println("• " + (esquema != null ? esquema + "." : "") + nomeTabela);
            }
            System.out.println("\nTotal de tabelas encontradas: " + nomeTabelas.size());
        }
    }

    private static void buscarTabelasPorTermos(Connection connection) throws SQLException {
        System.out.println("🔍 BUSCANDO TABELAS POR TERMOS RELEVANTES:");
        System.out.println("-".repeat(50));

        String[] termosRelevantes = {
            "fatura", "receber", "pagar", "coleta", "manifesto", "frete",
            "ocorrencia", "cotacao", "localizacao", "carga"
        };

        DatabaseMetaData metaData = connection.getMetaData();

        for (String termo : termosRelevantes) {
            System.out.println("🔎 Buscando por: '" + termo + "'");
            try (ResultSet tables = metaData.getTables(null, null, "%" + termo + "%", new String[]{"TABLE"})) {
                boolean encontrou = false;
                while (tables.next()) {
                    String nomeTabela = tables.getString("TABLE_NAME");
                    String esquema = tables.getString("TABLE_SCHEM");
                    System.out.println("  -> Encontrada: " + (esquema != null ? esquema + "." : "") + nomeTabela);
                    encontrou = true;
                }
                if (!encontrou) {
                    System.out.println("  -- Nenhuma tabela encontrada.");
                }
            }
        }
    }

    private static void verificarTabelasCriticas(Connection connection) {
        System.out.println("🎯 VERIFICANDO ACESSIBILIDADE DAS TABELAS DO PROJETO:");
        System.out.println("-".repeat(50));

        String[] tabelasDoProjeto = {
            "faturas_a_pagar", "faturas_a_receber", "ocorrencias",
            "coletas", "fretes", "cotacoes", "localizacao_cargas", "manifestos"
        };

        for (String nomeTabela : tabelasDoProjeto) {
            try {
                // Tenta executar uma query simples que não retorna dados.
                String sql = "SELECT 1 FROM " + nomeTabela + " WHERE 1=0";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.executeQuery();
                    System.out.println("✅ Tabela '" + nomeTabela + "' existe e é acessível.");
                }
            } catch (SQLException e) {
                System.out.println("❌ Tabela '" + nomeTabela + "' NÃO existe ou está inacessível.");
                System.out.println("   (Erro reportado: " + e.getSQLState() + " - " + e.getMessage() + ")");
            }
        }
    }
}