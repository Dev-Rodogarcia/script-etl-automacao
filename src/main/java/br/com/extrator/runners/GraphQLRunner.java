package br.com.extrator.runners;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import br.com.extrator.api.ClienteApiGraphQL;
import br.com.extrator.api.ResultadoExtracao;
import br.com.extrator.db.entity.ColetaEntity;
import br.com.extrator.db.entity.FreteEntity;
import br.com.extrator.db.entity.LogExtracaoEntity;
import br.com.extrator.db.repository.ColetaRepository;
import br.com.extrator.db.repository.FreteRepository;
import br.com.extrator.db.repository.LogExtracaoRepository;
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
        final LogExtracaoRepository logExtracaoRepository = new LogExtracaoRepository();

        final ColetaMapper coletaMapper = new ColetaMapper();
        final FreteMapper freteMapper = new FreteMapper();

        // Garante que a tabela log_extracoes existe
        logExtracaoRepository.criarTabelaSeNaoExistir();

        // Coletas
        System.out.println("\n📦 Extraindo Coletas...");
        LocalDateTime inicioColetas = LocalDateTime.now();
        try {
            final ResultadoExtracao<ColetaNodeDTO> resultadoColetas = clienteApiGraphQL.buscarColetas(dataInicio);
            final List<ColetaNodeDTO> coletasDTO = resultadoColetas.getDados();
            System.out.println("✓ Extraídas: " + coletasDTO.size() + " coletas" + 
                              (resultadoColetas.isCompleto() ? "" : " (INCOMPLETO: " + resultadoColetas.getMotivoInterrupcao() + ")"));
            
            int registrosSalvos = 0;
            if (!coletasDTO.isEmpty()) {
                final List<ColetaEntity> coletasEntities = coletasDTO.stream()
                    .map(coletaMapper::toEntity)
                    .collect(Collectors.toList());
                registrosSalvos = coletaRepository.salvar(coletasEntities);
                System.out.println("✓ Salvas: " + registrosSalvos + "/" + coletasDTO.size() + " coletas");
            }

            // Registrar no log
            String status = resultadoColetas.isCompleto() ? "COMPLETO" : "INCOMPLETO";
            String mensagem = resultadoColetas.isCompleto() ? 
                "Extração completa" : 
                "Extração incompleta: " + resultadoColetas.getMotivoInterrupcao();
            
            LogExtracaoEntity logColetas = new LogExtracaoEntity(
                "coletas",
                inicioColetas,
                LocalDateTime.now(),
                status,
                registrosSalvos,
                resultadoColetas.getPaginasProcessadas(),
                mensagem
            );
            logExtracaoRepository.gravarLogExtracao(logColetas);

        } catch (RuntimeException | java.sql.SQLException e) {
            // Registrar erro no log
            LogExtracaoEntity logErro = new LogExtracaoEntity(
                "coletas",
                inicioColetas,
                LocalDateTime.now(),
                "ERRO_API",
                0,
                0,
                "Erro: " + e.getMessage()
            );
            logExtracaoRepository.gravarLogExtracao(logErro);
            throw new RuntimeException("Falha na extração de coletas", e);
        }

        Thread.sleep(2000);

        // Fretes
        System.out.println("\n🚛 Extraindo Fretes...");
        LocalDateTime inicioFretes = LocalDateTime.now();
        try {
            final ResultadoExtracao<FreteNodeDTO> resultadoFretes = clienteApiGraphQL.buscarFretes(dataInicio);
            final List<FreteNodeDTO> fretesDTO = resultadoFretes.getDados();
            System.out.println("✓ Extraídos: " + fretesDTO.size() + " fretes" + 
                              (resultadoFretes.isCompleto() ? "" : " (INCOMPLETO: " + resultadoFretes.getMotivoInterrupcao() + ")"));
            
            int registrosSalvos = 0;
            if (!fretesDTO.isEmpty()) {
                final List<FreteEntity> fretesEntities = fretesDTO.stream()
                    .map(freteMapper::toEntity)
                    .collect(Collectors.toList());
                registrosSalvos = freteRepository.salvar(fretesEntities);
                System.out.println("✓ Salvos: " + registrosSalvos + "/" + fretesDTO.size() + " fretes");
            }

            // Registrar no log
            String status = resultadoFretes.isCompleto() ? "COMPLETO" : "INCOMPLETO";
            String mensagem = resultadoFretes.isCompleto() ? 
                "Extração completa" : 
                "Extração incompleta: " + resultadoFretes.getMotivoInterrupcao();
            
            LogExtracaoEntity logFretes = new LogExtracaoEntity(
                "fretes",
                inicioFretes,
                LocalDateTime.now(),
                status,
                registrosSalvos,
                resultadoFretes.getPaginasProcessadas(),
                mensagem
            );
            logExtracaoRepository.gravarLogExtracao(logFretes);

        } catch (RuntimeException | java.sql.SQLException e) {
            // Registrar erro no log
            LogExtracaoEntity logErro = new LogExtracaoEntity(
                "fretes",
                inicioFretes,
                LocalDateTime.now(),
                "ERRO_API",
                0,
                0,
                "Erro: " + e.getMessage()
            );
            logExtracaoRepository.gravarLogExtracao(logErro);
            throw new RuntimeException("Falha na extração de fretes", e);
        }
    }
}