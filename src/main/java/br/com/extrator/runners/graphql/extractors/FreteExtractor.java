package br.com.extrator.runners.graphql.extractors;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import br.com.extrator.api.ClienteApiGraphQL;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.FreteEntity;
import br.com.extrator.db.repository.FreteRepository;
import br.com.extrator.modelo.graphql.fretes.FreteMapper;
import br.com.extrator.modelo.graphql.fretes.FreteNodeDTO;
import br.com.extrator.runners.common.ConstantesExtracao;
import br.com.extrator.runners.common.EntityExtractor;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Extractor para entidade Fretes (GraphQL).
 */
public class FreteExtractor implements EntityExtractor<FreteNodeDTO> {
    
    private final ClienteApiGraphQL apiClient;
    private final FreteRepository repository;
    private final FreteMapper mapper;
    
    public FreteExtractor(final ClienteApiGraphQL apiClient,
                         final FreteRepository repository,
                         final FreteMapper mapper) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.mapper = mapper;
    }
    
    @Override
    public ResultadoExtracao<FreteNodeDTO> extract(final LocalDate dataInicio, final LocalDate dataFim) {
        return apiClient.buscarFretes(dataInicio, dataFim);
    }
    
    @Override
    public int save(final List<FreteNodeDTO> dtos) throws java.sql.SQLException {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }
        
        final List<FreteEntity> entities = dtos.stream()
            .map(mapper::toEntity)
            .collect(Collectors.toList());
        
        return repository.salvar(entities);
    }
    
    @Override
    public String getEntityName() {
        return ConstantesEntidades.FRETES;
    }
    
    @Override
    public String getEmoji() {
        return ConstantesExtracao.EMOJI_FRETES;
    }
}
