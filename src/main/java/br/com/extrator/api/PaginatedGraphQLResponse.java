package br.com.extrator.api;

import java.util.List;

/**
 * Classe auxiliar para encapsular a resposta paginada de uma query GraphQL.
 * Contém os dados da página atual e as informações de paginação necessárias
 * para continuar buscando as próximas páginas.
 */
public class PaginatedGraphQLResponse<T> {
    
    private final List<T> entidades;
    private final boolean hasNextPage;
    private final String endCursor;
    
    /**
     * Construtor da resposta paginada
     * 
     * @param entidades Lista de entidades da página atual
     * @param hasNextPage Indica se há próxima página disponível
     * @param endCursor Cursor para buscar a próxima página
     */
    public PaginatedGraphQLResponse(List<T> entidades, boolean hasNextPage, String endCursor) {
        this.entidades = entidades;
        this.hasNextPage = hasNextPage;
        this.endCursor = endCursor;
    }
    
    /**
     * @return Lista de entidades da página atual
     */
    public List<T> getEntidades() {
        return entidades;
    }
    
    /**
     * @return true se há próxima página disponível, false caso contrário
     */
    public boolean getHasNextPage() {
        return hasNextPage;
    }
    
    /**
     * @return Cursor para buscar a próxima página
     */
    public String getEndCursor() {
        return endCursor;
    }
}