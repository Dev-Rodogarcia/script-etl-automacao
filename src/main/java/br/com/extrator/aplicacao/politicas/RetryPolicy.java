package br.com.extrator.aplicacao.politicas;

@FunctionalInterface
public interface RetryPolicy {
    <T> T executar(CheckedSupplier<T> supplier, String operationName) throws Exception;

    @FunctionalInterface
    interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}


