/* ==[DOC-FILE]===============================================================
Arquivo : src/main/java/br/com/extrator/comandos/spi/ComandoProvider.java
Classe  : ComandoProvider (interface)
Pacote  : br.com.extrator.comandos.spi
Modulo  : Comandos CLI (extensibilidade)
Papel   : Define SPI para registro declarativo de comandos no CommandRegistry.

Conecta com:
- CommandRegistry
- Comando
- META-INF/services/br.com.extrator.comandos.spi.ComandoProvider

Fluxo geral:
1) Implementacao SPI declara mapa de comandos.
2) ServiceLoader descobre providers no classpath.
3) Registry incorpora comandos sem alterar codigo central.

Estrutura interna:
Metodos principais:
- comandos(...0 args): retorna comandos expostos pelo provider.
Atributos-chave:
- Sem atributos (interface).
[DOC-FILE-END]============================================================== */

package br.com.extrator.comandos.spi;

import java.util.Map;

import br.com.extrator.comandos.base.Comando;

/**
 * SPI para registro declarativo de comandos CLI.
 *
 * Implementacoes devem ser publicas e registradas em:
 * META-INF/services/br.com.extrator.comandos.spi.ComandoProvider
 */
public interface ComandoProvider {
    /**
     * Retorna o mapa de comandos que serao incorporados ao registry principal.
     *
     * Chave esperada: nome do comando (ex.: "--meu-comando")
     */
    Map<String, Comando> comandos();
}
