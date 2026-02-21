package br.com.extrator.modelo.dataexport.faturaporcliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class FaturaPorClienteMapperTest {

    private final FaturaPorClienteMapper mapper = new FaturaPorClienteMapper();

    @Test
    void deveUsarNfseQuandoDisponivel() {
        final FaturaPorClienteDTO dto = new FaturaPorClienteDTO();
        dto.setNfseNumber(123456L);
        dto.setCteKey("35100000000000000000000000000000000000000000");

        final String uniqueId = mapper.calcularIdentificadorUnico(dto);

        assertEquals("NFSE-123456", uniqueId);
    }

    @Test
    void deveUsarChaveCteQuandoNaoHaNfse() {
        final FaturaPorClienteDTO dto = new FaturaPorClienteDTO();
        dto.setCteKey("35100000000000000000000000000000000000000000");

        final String uniqueId = mapper.calcularIdentificadorUnico(dto);

        assertEquals("35100000000000000000000000000000000000000000", uniqueId);
    }

    @Test
    void deveGerarHashDeterministicoQuandoNaoHaChaveNatural() {
        final FaturaPorClienteDTO dto = criarDtoSemChaveNatural();

        final String uniqueIdPrimeiraExecucao = mapper.calcularIdentificadorUnico(dto);
        final String uniqueIdSegundaExecucao = mapper.calcularIdentificadorUnico(dto);

        assertEquals(uniqueIdPrimeiraExecucao, uniqueIdSegundaExecucao);
        assertTrue(uniqueIdPrimeiraExecucao.startsWith("FPC-HASH-"));
        assertTrue(uniqueIdPrimeiraExecucao.length() <= 100);
    }

    @Test
    void deveAlterarHashQuandoCampoCanonicoMuda() {
        final FaturaPorClienteDTO dtoA = criarDtoSemChaveNatural();
        final FaturaPorClienteDTO dtoB = criarDtoSemChaveNatural();
        dtoB.setValorFrete("1020.75");

        final String uniqueIdA = mapper.calcularIdentificadorUnico(dtoA);
        final String uniqueIdB = mapper.calcularIdentificadorUnico(dtoB);

        assertNotEquals(uniqueIdA, uniqueIdB);
    }

    private FaturaPorClienteDTO criarDtoSemChaveNatural() {
        final FaturaPorClienteDTO dto = new FaturaPorClienteDTO();
        dto.setNfseNumber(null);
        dto.setCteKey(null);
        dto.setFaturaDocument(null);
        dto.setBillingId(null);
        dto.setValorFrete("1000.50");
        dto.setThirdPartyCtesValue("0.00");
        dto.setTipoFrete("Freight::Normal");
        dto.setFilial("Matriz");
        dto.setEstado("SP");
        dto.setClassificacao("Padrao");
        dto.setPagadorNome("Cliente Teste");
        dto.setPagadorDocumento("12345678000190");
        dto.setRemetenteNome("Remetente");
        dto.setRemetenteDocumento("11111111000191");
        dto.setDestinatarioNome("Destinatario");
        dto.setDestinatarioDocumento("22222222000192");
        dto.setVendedorNome("Vendedor");
        dto.setNotasFiscais(List.of("NF-1", "NF-2"));
        dto.setPedidosCliente(List.of("PED-1", "PED-2"));
        return dto;
    }
}
