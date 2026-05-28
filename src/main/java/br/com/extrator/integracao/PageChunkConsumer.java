package br.com.extrator.integracao;

import java.util.List;

@FunctionalInterface
public interface PageChunkConsumer<T> {
    void process(List<T> registros) throws Exception;
}
