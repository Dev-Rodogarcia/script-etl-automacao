package br.com.extrator.runners;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import br.com.extrator.api.ClienteApiRest;
import br.com.extrator.db.entity.FaturaAPagarEntity;
import br.com.extrator.db.entity.FaturaAReceberEntity;
import br.com.extrator.db.entity.OcorrenciaEntity;
import br.com.extrator.db.repository.FaturaAPagarRepository;
import br.com.extrator.db.repository.FaturaAReceberRepository;
import br.com.extrator.db.repository.OcorrenciaRepository;
import br.com.extrator.modelo.rest.faturaspagar.FaturaAPagarDTO;
import br.com.extrator.modelo.rest.faturaspagar.FaturaAPagarMapper;
import br.com.extrator.modelo.rest.faturasreceber.FaturaAReceberDTO;
import br.com.extrator.modelo.rest.faturasreceber.FaturaAReceberMapper;
import br.com.extrator.modelo.rest.ocorrencias.OcorrenciaDTO;
import br.com.extrator.modelo.rest.ocorrencias.OcorrenciaMapper;

/**
 * Runner independente para a API REST (Faturas e Ocorrências).
 * Reutiliza a lógica de teste/execução direta e respeita rate limit entre chamadas.
 */
public final class RestRunner {

    private RestRunner() {}

    public static void executar(final LocalDate dataInicio) throws Exception {
        System.out.println("🔄 Executando runner REST...");

        // Valida conexão com banco
        br.com.extrator.util.CarregadorConfig.validarConexaoBancoDados();

        // Inicializa cliente, repositórios e mappers
        final ClienteApiRest clienteApiRest = new ClienteApiRest();
        final FaturaAReceberRepository faturaAReceberRepository = new FaturaAReceberRepository();
        final FaturaAPagarRepository faturaAPagarRepository = new FaturaAPagarRepository();
        final OcorrenciaRepository ocorrenciaRepository = new OcorrenciaRepository();

        final FaturaAReceberMapper faturaAReceberMapper = new FaturaAReceberMapper();
        final FaturaAPagarMapper faturaAPagarMapper = new FaturaAPagarMapper();
        final OcorrenciaMapper ocorrenciaMapper = new OcorrenciaMapper();

        // Faturas a Receber
        System.out.println("\n📋 Extraindo Faturas a Receber...");
        final List<FaturaAReceberDTO> faturasReceberDTOs = clienteApiRest.buscarFaturasAReceber(dataInicio);
        System.out.println("✓ Extraídas: " + faturasReceberDTOs.size() + " faturas a receber");
        if (!faturasReceberDTOs.isEmpty()) {
            final List<FaturaAReceberEntity> faturasReceberEntities = faturasReceberDTOs.stream()
                .map(faturaAReceberMapper::toEntity)
                .collect(Collectors.toList());
            final int processados = faturaAReceberRepository.salvar(faturasReceberEntities);
            System.out.println("✓ Salvas: " + processados + "/" + faturasReceberDTOs.size() + " faturas a receber");
        }

        Thread.sleep(2000); // Rate limit

        // Faturas a Pagar
        System.out.println("\n💰 Extraindo Faturas a Pagar...");
        final List<FaturaAPagarDTO> faturasPagarDTOs = clienteApiRest.buscarFaturasAPagar(dataInicio);
        System.out.println("✓ Extraídas: " + faturasPagarDTOs.size() + " faturas a pagar");
        if (!faturasPagarDTOs.isEmpty()) {
            final List<FaturaAPagarEntity> faturasPagarEntities = faturasPagarDTOs.stream()
                .map(dto -> {
                    // Busca os itens/parcelas da fatura específica
                    String itensJson = clienteApiRest.buscarItensFaturaAPagar(dto.getId());
                    return faturaAPagarMapper.toEntity(dto, itensJson);
                })
                .collect(Collectors.toList());
            final int processados = faturaAPagarRepository.salvar(faturasPagarEntities);
            System.out.println("✓ Salvas: " + processados + "/" + faturasPagarDTOs.size() + " faturas a pagar");
        }

        Thread.sleep(2000); // Rate limit

        // Ocorrências
        System.out.println("\n📝 Extraindo Ocorrências...");
        final List<OcorrenciaDTO> ocorrenciasDTOs = clienteApiRest.buscarOcorrencias(dataInicio);
        System.out.println("✓ Extraídas: " + ocorrenciasDTOs.size() + " ocorrências");
        if (!ocorrenciasDTOs.isEmpty()) {
            final List<OcorrenciaEntity> ocorrenciasEntities = ocorrenciasDTOs.stream()
                .map(ocorrenciaMapper::toEntity)
                .collect(Collectors.toList());
            final int processados = ocorrenciaRepository.salvar(ocorrenciasEntities);
            System.out.println("✓ Salvas: " + processados + "/" + ocorrenciasDTOs.size() + " ocorrências");
        }
    }
}