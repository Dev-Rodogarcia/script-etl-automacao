/* ==[DOC-FILE]===============================================================
Arquivo : src/test/java/br/com/extrator/comandos/extracao/daemon/DaemonLifecycleServiceTest.java
Classe  : DaemonLifecycleServiceTest (class)
Pacote  : br.com.extrator.comandos.cli.extracao.daemon
Modulo  : Teste automatizado
Papel   : Valida comportamento da unidade DaemonLifecycleService.

Conecta com:
- Sem dependencia interna explicita (classe isolada ou foco em libs externas).

Fluxo geral:
1) Prepara cenarios e dados de teste.
2) Executa casos para validar comportamento de DaemonLifecycleService.
3) Assegura regressao controlada nas regras principais.

Estrutura interna:
Metodos principais:
- novoService(): realiza operacao relacionada a "novo service".
Atributos-chave:
- Atributos nao mapeados automaticamente; consulte a implementacao abaixo.
[DOC-FILE-END]============================================================== */

package br.com.extrator.comandos.cli.extracao.daemon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DaemonLifecycleServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void deveMontarComandoFilhoComFlagLoopDaemon() throws Exception {
        final DaemonLifecycleService service = novoService();

        final List<String> comando = service.construirComandoFilho(true);

        assertFalse(comando.isEmpty(), "Comando do processo filho nao pode ser vazio");
        assertTrue(comando.stream().anyMatch(arg -> arg.contains("java")), "Comando deve conter executavel Java");
        assertTrue(comando.contains("--loop-daemon-run"), "Comando deve conter flag de loop daemon");
        assertFalse(comando.contains("--sem-faturas-graphql"), "Modo padrao deve incluir faturas GraphQL");
    }

    @Test
    void deveIncluirFlagSemFaturasQuandoDesabilitado() throws Exception {
        final DaemonLifecycleService service = novoService();

        final List<String> comando = service.construirComandoFilho(false);

        assertTrue(comando.contains("--loop-daemon-run"), "Comando deve conter flag de loop daemon");
        assertTrue(comando.contains("--sem-faturas-graphql"), "Comando deve carregar flag de desabilitar faturas GraphQL");
    }

    @Test
    void deveReconhecerPidPersistidoComoDaemonAtivoQuandoEstadoIndicarExecucao() {
        final DaemonStateStore store = novoStore();
        final DaemonLifecycleService service = novoService(store);
        final long pidAtual = ProcessHandle.current().pid();

        garantirDiretorio(store);
        store.syncPidFile(pidAtual);
        store.saveState("WAITING_NEXT_CYCLE", pidAtual, "Daemon aguardando proximo ciclo.", null, null);

        assertTrue(
            service.processoEhLoopDaemonAtivo(pidAtual),
            "PID persistido com estado ativo deve ser reconhecido mesmo quando o comando do processo nao estiver acessivel"
        );
    }

    @Test
    void naoDeveReconhecerPidPersistidoQuandoEstadoEstiverParado() {
        final DaemonStateStore store = novoStore();
        final DaemonLifecycleService service = novoService(store);
        final long pidAtual = ProcessHandle.current().pid();

        garantirDiretorio(store);
        store.syncPidFile(pidAtual);
        store.saveState("STOPPED", pidAtual, "Daemon parado.", null, null);

        assertFalse(
            service.processoEhLoopDaemonAtivo(pidAtual),
            "Estado parado nao deve tratar PID persistido como daemon ativo"
        );
    }

    private DaemonLifecycleService novoService() {
        return novoService(novoStore());
    }

    private DaemonLifecycleService novoService(final DaemonStateStore store) {
        return new DaemonLifecycleService(
            store,
            tempDir.resolve("daemon.log"),
            tempDir.resolve("runtime")
        );
    }

    private DaemonStateStore novoStore() {
        return new DaemonStateStore(
            tempDir.resolve("daemon"),
            tempDir.resolve("daemon").resolve("loop_daemon.state"),
            tempDir.resolve("daemon").resolve("loop_daemon.pid"),
            tempDir.resolve("daemon").resolve("loop_daemon.stop"),
            tempDir.resolve("daemon").resolve("loop_daemon.force_run")
        );
    }

    private void garantirDiretorio(final DaemonStateStore store) {
        try {
            store.ensureDaemonDirectory();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
