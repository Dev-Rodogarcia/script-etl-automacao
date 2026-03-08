package br.com.extrator.aplicacao.portas;

public interface ConfigPort {
    String obterTexto(String key, String defaultValue);

    int obterInteiro(String key, int defaultValue);

    long obterLongo(String key, long defaultValue);

    double obterDecimal(String key, double defaultValue);

    boolean obterBooleano(String key, boolean defaultValue);
}
