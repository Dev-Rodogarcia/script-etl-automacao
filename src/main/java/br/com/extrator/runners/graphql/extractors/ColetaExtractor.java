package br.com.extrator.runners.graphql.extractors;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import br.com.extrator.api.ClienteApiGraphQL;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.ColetaEntity;
import br.com.extrator.db.repository.ColetaRepository;
import br.com.extrator.modelo.graphql.coletas.ColetaMapper;
import br.com.extrator.modelo.graphql.coletas.ColetaNodeDTO;
import br.com.extrator.runners.common.ConstantesExtracao;
import br.com.extrator.runners.common.EntityExtractor;
import br.com.extrator.util.validacao.ConstantesEntidades;

/**
 * Extractor para entidade Coletas (GraphQL).
 */
public class ColetaExtractor implements EntityExtractor<ColetaNodeDTO> {
    
    private final ClienteApiGraphQL apiClient;
    private final ColetaRepository repository;
    private final ColetaMapper mapper;
    
    public ColetaExtractor(final ClienteApiGraphQL apiClient,
                          final ColetaRepository repository,
                          final ColetaMapper mapper) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.mapper = mapper;
    }
    
    @Override
    public ResultadoExtracao<ColetaNodeDTO> extract(final LocalDate dataInicio, final LocalDate dataFim) {
        return apiClient.buscarColetas(dataInicio, dataFim);
    }
    
    @Override
    public int save(final List<ColetaNodeDTO> dtos) throws java.sql.SQLException {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }
        
        final List<ColetaEntity> entities = dtos.stream()
            .map(mapper::toEntity)
            .collect(Collectors.toList());
        
        return repository.salvar(entities);
    }
    
    @Override
    public String getEntityName() {
        return ConstantesEntidades.COLETAS;
    }
    
    @Override
    public String getEmoji() {
        return ConstantesExtracao.EMOJI_COLETAS;
    }
}
