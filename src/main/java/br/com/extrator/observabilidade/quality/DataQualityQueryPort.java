package br.com.extrator.observabilidade.quality;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface DataQualityQueryPort {
    long contarDuplicidadesChaveNatural(String entidade, LocalDate dataInicio, LocalDate dataFim);

    long contarLinhasIncompletas(String entidade, LocalDate dataInicio, LocalDate dataFim);

    LocalDateTime buscarTimestampMaisRecente(String entidade);

    long contarQuebrasReferenciais(String entidade);

    String detectarVersaoSchema(String entidade);
}


