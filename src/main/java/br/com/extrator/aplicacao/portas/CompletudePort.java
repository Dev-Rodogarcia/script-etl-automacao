package br.com.extrator.aplicacao.portas;

import java.time.LocalDate;
import java.util.Map;

/**
 * Porta para validacao de completude dos dados extraidos (origem x destino).
 */
public interface CompletudePort {

    Map<String, StatusCompletude> validarCompletudePorLogs(LocalDate dataReferencia);

    enum StatusCompletude {
        OK,
        INCOMPLETO,
        DUPLICADOS,
        ERRO
    }
}
