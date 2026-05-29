/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/comandos/cli/utilitarios/TestarApiComando.java
Classe  : TestarApiComando (command), ParsedArgs (record)
Pacote  : br.com.extrator.comandos.cli.utilitarios
Modulo  : CLI - Utilitario
Papel   : Comando para teste de conectividade/resposta de APIs (GraphQL ou DataExport).
Conecta com:
- br.com.extrator.aplicacao.extracao.TesteApiUseCase
- br.com.extrator.aplicacao.extracao.TesteApiRequest
- br.com.extrator.comandos.cli.base.Comando
Fluxo geral:
1) executar() valida args (tipo API obrigatório)
2) parseArgs() extrai entidade opcional
3) Delegação a TesteApiUseCase.executar() com TesteApiRequest
Estrutura interna:
Atributos: useCase
Metodos: executar(), parseArgs()
[DOC-FILE-END]============================================================== */
package br.com.extrator.comandos.cli.utilitarios;

import br.com.extrator.aplicacao.extracao.TesteApiRequest;
import br.com.extrator.aplicacao.extracao.TesteApiUseCase;
import br.com.extrator.comandos.cli.base.Comando;

public class TestarApiComando implements Comando {
    private final TesteApiUseCase useCase;

    public TestarApiComando() {
        this(new TesteApiUseCase());
    }

    TestarApiComando(final TesteApiUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public void executar(final String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("ERRO: Tipo de API nao especificado.");
            System.err.println("Uso: --testar-api <tipo> [entidade]");
            System.err.println("Tipos validos: graphql, dataexport, raster");
            throw new IllegalArgumentException("Tipo de API nao especificado. Tipos validos: graphql, dataexport, raster");
        }

        final String tipoApi = args[1];
        final String entidade = parseEntidade(args);

        useCase.executar(
            new TesteApiRequest(
                tipoApi,
                entidade
            )
        );
    }

    private String parseEntidade(final String[] args) {
        if (args.length > 3) {
            throw new IllegalArgumentException(
                "Argumentos invalidos para --testar-api. Uso: --testar-api <tipo> [entidade]"
            );
        }

        return args.length == 3 ? args[2] : null;
    }
}
