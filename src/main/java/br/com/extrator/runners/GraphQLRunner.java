package br.com.extrator.runners;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import br.com.extrator.api.ClienteApiGraphQL;
import br.com.extrator.db.entity.ColetaEntity;
import br.com.extrator.db.entity.FreteEntity;
import br.com.extrator.db.repository.ColetaRepository;
import br.com.extrator.db.repository.FreteRepository;
import br.com.extrator.modelo.graphql.coletas.ColetaNodeDTO;
import br.com.extrator.modelo.graphql.coletas.ColetaMapper;
import br.com.extrator.modelo.graphql.fretes.FreteNodeDTO;
import br.com.extrator.modelo.graphql.fretes.FreteMapper;

/**
 * Runner independente para a API GraphQL (Coletas e Fretes).
 */
public final class GraphQLRunner {

    private GraphQLRunner() {}

    public static void executar(final LocalDate dataInicio) throws Exception {
        System.out.println("🔄 Executando runner GraphQL...");

        br.com.extrator.util.CarregadorConfig.validarConexaoBancoDados();

        final ClienteApiGraphQL clienteApiGraphQL = new ClienteApiGraphQL();
        final ColetaRepository coletaRepository = new ColetaRepository();
        final FreteRepository freteRepository = new FreteRepository();

        final ColetaMapper coletaMapper = new ColetaMapper();
        final FreteMapper freteMapper = new FreteMapper();

        // Coletas
        System.out.println("\n🚚 Extraindo Coletas...");
        final List<ColetaNodeDTO> coletasDTO = clienteApiGraphQL.buscarColetas(dataInicio);
        System.out.println("✓ Extraídas: " + coletasDTO.size() + " coletas");
        if (!coletasDTO.isEmpty()) {
            final List<ColetaEntity> coletasEntities = coletasDTO.stream()
                .map(coletaMapper::toEntity)
                .collect(Collectors.toList());
            final int processados = coletaRepository.salvar(coletasEntities);
            System.out.println("✓ Salvas: " + processados + "/" + coletasDTO.size() + " coletas");
        }

        Thread.sleep(2000);

        // Fretes
        System.out.println("\n📦 Extraindo Fretes...");
        final List<FreteNodeDTO> fretesDTO = clienteApiGraphQL.buscarFretes(dataInicio);
        System.out.println("✓ Extraídos: " + fretesDTO.size() + " fretes");
        if (!fretesDTO.isEmpty()) {
            final List<FreteEntity> fretesEntities = fretesDTO.stream()
                .map(freteMapper::toEntity)
                .collect(Collectors.toList());
            final int processados = freteRepository.salvar(fretesEntities);
            System.out.println("✓ Salvos: " + processados + "/" + fretesDTO.size() + " fretes");
        }
    }
}