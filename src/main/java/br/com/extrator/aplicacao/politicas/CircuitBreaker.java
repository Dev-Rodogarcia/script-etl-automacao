package br.com.extrator.aplicacao.politicas;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import br.com.extrator.aplicacao.portas.ClockPort;

public class CircuitBreaker {
    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final int failureThreshold;
    private final Duration openDuration;
    private final ClockPort clock;
    private final Map<String, InternalState> states = new ConcurrentHashMap<>();

    public CircuitBreaker(final int failureThreshold, final Duration openDuration, final ClockPort clock) {
        this.failureThreshold = Math.max(1, failureThreshold);
        this.openDuration = openDuration == null ? Duration.ofSeconds(60) : openDuration;
        this.clock = clock;
    }

    public boolean permite(final String key) {
        final InternalState state = states.computeIfAbsent(normalize(key), ignored -> new InternalState());
        if (state.state == State.CLOSED) {
            return true;
        }
        if (state.state == State.OPEN) {
            final LocalDateTime agora = clock.agora();
            if (state.openedAt == null || Duration.between(state.openedAt, agora).compareTo(openDuration) >= 0) {
                state.state = State.HALF_OPEN;
                return true;
            }
            return false;
        }
        return true;
    }

    public void registrarSucesso(final String key) {
        final InternalState state = states.computeIfAbsent(normalize(key), ignored -> new InternalState());
        state.failureCount = 0;
        state.state = State.CLOSED;
        state.openedAt = null;
    }

    public void registrarFalha(final String key) {
        final InternalState state = states.computeIfAbsent(normalize(key), ignored -> new InternalState());
        state.failureCount++;
        if (state.failureCount >= failureThreshold) {
            state.state = State.OPEN;
            state.openedAt = clock.agora();
        }
    }

    public State estadoDe(final String key) {
        return states.getOrDefault(normalize(key), new InternalState()).state;
    }

    private String normalize(final String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }

    private static final class InternalState {
        private int failureCount = 0;
        private LocalDateTime openedAt;
        private State state = State.CLOSED;
    }
}



