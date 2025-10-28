package br.com.extrator.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Classe centralizada para gerenciar e fornecer conexões com o banco de dados.
 * Garante que toda a aplicação (produção e ferramentas) utilize a mesma
 * fonte de configuração para as credenciais do banco.
 */
public class GerenciadorConexao {

    // Lê as credenciais de uma única fonte (variáveis de ambiente, conforme projeto original).
    // Se sua aplicação usa um arquivo .properties, a lógica de leitura deve ser centralizada aqui.
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    /**
     * Obtém uma nova conexão com o banco de dados.
     *
     * @return Uma instância de Connection.
     * @throws SQLException Se as credenciais não estiverem configuradas ou a conexão falhar.
     */
    public static Connection obterConexao() throws SQLException {
        if (DB_URL == null || DB_URL.trim().isEmpty() ||
            DB_USER == null || DB_USER.trim().isEmpty() ||
            DB_PASSWORD == null) {
            
            throw new SQLException("Variáveis de ambiente do banco (DB_URL, DB_USER, DB_PASSWORD) não estão configuradas ou estão vazias.");
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
