/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/modelo/graphql/usuarios/UsuarioSistemaMapper.java
Classe  : UsuarioSistemaMapper (class)
Pacote  : br.com.extrator.dominio.graphql.usuarios
Modulo  : DTO/Mapper GraphQL
Papel   : Implementa responsabilidade de usuario sistema mapper.

Conecta com:
- UsuarioSistemaEntity (db.entity)

Fluxo geral:
1) Modela payloads da API GraphQL.
2) Mapeia estrutura remota para modelo interno.
3) Apoia persistencia e validacao do extrator.

Estrutura interna:
Metodos principais:
- toEntity(...1 args): realiza operacao relacionada a "to entity".
Atributos-chave:
- Atributos nao mapeados automaticamente; consulte a implementacao abaixo.
[DOC-FILE-END]============================================================== */

package br.com.extrator.integracao.mapeamento.graphql.usuarios;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import br.com.extrator.dominio.graphql.usuarios.IndividualNodeDTO;
import br.com.extrator.persistencia.entidade.UsuarioSistemaEntity;
import br.com.extrator.suporte.formatacao.FormatadorData;
import br.com.extrator.suporte.tempo.RelogioSistema;

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

        final LocalDateTime observadoEm = RelogioSistema.agora();
        final OffsetDateTime origemAtualizada = FormatadorData.parseOffsetDateTime(dto.getUpdatedAt());
        final UsuarioSistemaEntity entity = new UsuarioSistemaEntity();
        entity.setUserId(dto.getId());
        entity.setNome(dto.getName());
        entity.setAtivo(true);
        entity.setOrigemAtualizadoEm(origemAtualizada != null ? origemAtualizada.toLocalDateTime() : null);
        entity.setDataAtualizacao(observadoEm);
        entity.setUltimaExtracaoEm(observadoEm);
        return entity;
    }
}
