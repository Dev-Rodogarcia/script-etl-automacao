package br.com.extrator.observabilidade.adaptador;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

import br.com.extrator.aplicacao.portas.CompletudePort;
import br.com.extrator.observabilidade.servicos.CompletudeValidator;

/**
 * Adapter que implementa CompletudePort usando CompletudeValidator.
 */
public class CompletudePortAdapter implements CompletudePort {

    private final CompletudeValidator validator;

    public CompletudePortAdapter() {
        this.validator = new CompletudeValidator();
    }

    @Override
    public Map<String, StatusCompletude> validarCompletudePorLogs(final LocalDate dataReferencia) {
        final Map<String, CompletudeValidator.StatusValidacao> resultado =
            validator.validarCompletudePorLogs(dataReferencia);
        return resultado.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> mapStatus(e.getValue())
            ));
    }

    private StatusCompletude mapStatus(final CompletudeValidator.StatusValidacao status) {
        if (status == null) {
            return StatusCompletude.ERRO;
        }
        return switch (status) {
            case OK -> StatusCompletude.OK;
            case INCOMPLETO -> StatusCompletude.INCOMPLETO;
            case DUPLICADOS -> StatusCompletude.DUPLICADOS;
            case ERRO -> StatusCompletude.ERRO;
        };
    }
}
