package br.com.extrator.aplicacao.portas;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ClockPort {
    LocalDate hoje();

    LocalDateTime agora();

    void dormir(Duration duration) throws InterruptedException;
}
