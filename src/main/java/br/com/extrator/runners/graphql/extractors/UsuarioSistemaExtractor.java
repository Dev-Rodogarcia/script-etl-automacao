package br.com.extrator.runners.graphql.extractors;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import br.com.extrator.api.ClienteApiGraphQL;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.UsuarioSistemaEntity;
import br.com.extrator.db.repository.UsuarioSistemaRepository;
import br.com.extrator.modelo.graphql.usuarios.IndividualNodeDTO;
import br.com.extrator.modelo.graphql.usuarios.UsuarioSistemaMapper;
import br.com.extrator.runners.common.ConstantesExtracao;
import br.com.extrator.runners.common.EntityExtractor;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Extractor para entidade Usuários do Sistema (Individual - GraphQL).
 * Não utiliza filtro de data, apenas filtra por enabled: true.
 */
public class UsuarioSistemaExtractor implements EntityExtractor<IndividualNodeDTO> {
    
    private final ClienteApiGraphQL apiClient;
    private final UsuarioSistemaRepository repository;
    private final UsuarioSistemaMapper mapper;
    
    public UsuarioSistemaExtractor(final ClienteApiGraphQL apiClient,
                                  final UsuarioSistemaRepository repository,
                                  final UsuarioSistemaMapper mapper) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.mapper = mapper;
    }
    
    @Override
    public ResultadoExtracao<IndividualNodeDTO> extract(final LocalDate dataInicio, final LocalDate dataFim) {
        // Usuários não usam filtro de data, apenas enabled: true
        return apiClient.buscarUsuariosSistema();
    }
    
    @Override
    public int save(final List<IndividualNodeDTO> dtos) throws java.sql.SQLException {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }
        
        final List<UsuarioSistemaEntity> entities = dtos.stream()
            .map(mapper::toEntity)
            .collect(Collectors.toList());
        
        return repository.salvar(entities);
    }
    
    @Override
    public String getEntityName() {
        return ConstantesEntidades.USUARIOS_SISTEMA;
    }
    
    @Override
    public String getEmoji() {
        return ConstantesExtracao.EMOJI_USUARIOS;
    }
}
