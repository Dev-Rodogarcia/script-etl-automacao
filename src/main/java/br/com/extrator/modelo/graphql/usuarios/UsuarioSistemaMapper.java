package br.com.extrator.modelo.graphql.usuarios;

import java.time.LocalDateTime;

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
        entity.setDataAtualizacao(LocalDateTime.now());
        return entity;
    }
}
