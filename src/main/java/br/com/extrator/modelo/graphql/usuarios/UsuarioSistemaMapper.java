package br.com.extrator.modelo.graphql.usuarios;

import br.com.extrator.db.entity.UsuarioSistemaEntity;

/**
 * Mapper responsável por converter IndividualNodeDTO (GraphQL) para UsuarioSistemaEntity (Banco de Dados).
 */
public class UsuarioSistemaMapper {

    /**
     * Converte um IndividualNodeDTO para UsuarioSistemaEntity.
     * 
     * @param dto DTO do GraphQL
     * @return Entity para persistência no banco
     */
    public UsuarioSistemaEntity toEntity(final IndividualNodeDTO dto) {
        if (dto == null) {
            return null;
        }

        final UsuarioSistemaEntity entity = new UsuarioSistemaEntity();
        entity.setUserId(dto.getId());
        entity.setNome(dto.getName());
        // data_atualizacao será definida pelo SQL MERGE (GETDATE())
        return entity;
    }
}
