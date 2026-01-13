package br.com.extrator.comandos.base;

/**
 * Interface que define o contrato comum para todos os comandos do sistema.
 * 
 * Implementa o padrão Command, permitindo que a classe Main seja um dispatcher
 * puro que delega a execução para classes especializadas.
 */
public interface Comando {
    
    /**
     * Executa o comando com os argumentos fornecidos.
     * 
     * @param args Array completo de argumentos da linha de comando
     * @throws Exception Se ocorrer algum erro durante a execução
     */
    void executar(String[] args) throws Exception;
}