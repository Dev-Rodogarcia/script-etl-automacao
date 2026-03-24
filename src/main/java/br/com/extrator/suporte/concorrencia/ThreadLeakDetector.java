package br.com.extrator.suporte.concorrencia;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ThreadLeakDetector {
    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

    private ThreadLeakDetector() {
    }

    public static Snapshot captureByPrefix(final String... prefixes) {
        final Map<Long, ThreadSnapshot> threads = new LinkedHashMap<>();
        final Map<Thread, StackTraceElement[]> liveThreads = Thread.getAllStackTraces();
        for (final Thread thread : liveThreads.keySet()) {
            if (thread == null || !thread.isAlive() || thread.getState() == Thread.State.TERMINATED) {
                continue;
            }
            if (!matchesPrefix(thread.getName(), prefixes)) {
                continue;
            }
            final ThreadInfo info = THREAD_MX_BEAN.getThreadInfo(thread.getId(), 10);
            if (info != null && info.getThreadState() == Thread.State.TERMINATED) {
                continue;
            }
            threads.put(thread.getId(), ThreadSnapshot.from(thread, info));
        }
        return new Snapshot(threads, THREAD_MX_BEAN.getThreadCount());
    }

    public static LeakReport detectNewThreads(final Snapshot before, final Snapshot after) {
        final Map<Long, ThreadSnapshot> leaked = new LinkedHashMap<>();
        for (final Map.Entry<Long, ThreadSnapshot> entry : after.threads().entrySet()) {
            if (!before.threads().containsKey(entry.getKey())) {
                leaked.put(entry.getKey(), entry.getValue());
            }
        }
        return new LeakReport(
            List.copyOf(leaked.values()),
            Math.max(0, after.totalThreadCount() - before.totalThreadCount())
        );
    }

    public static LeakReport inspectThread(final Thread thread) {
        if (thread == null || !thread.isAlive() || thread.getState() == Thread.State.TERMINATED) {
            return LeakReport.none();
        }
        final ThreadInfo info = THREAD_MX_BEAN.getThreadInfo(thread.getId(), 10);
        if (info != null && info.getThreadState() == Thread.State.TERMINATED) {
            return LeakReport.none();
        }
        return new LeakReport(List.of(ThreadSnapshot.from(thread, info)), 1);
    }

    private static boolean matchesPrefix(final String threadName, final String... prefixes) {
        if (threadName == null || threadName.isBlank()) {
            return false;
        }
        if (prefixes == null || prefixes.length == 0) {
            return true;
        }
        return Arrays.stream(prefixes)
            .filter(Objects::nonNull)
            .map(prefix -> prefix.toLowerCase(Locale.ROOT))
            .anyMatch(prefix -> !prefix.isBlank() && threadName.toLowerCase(Locale.ROOT).startsWith(prefix));
    }

    public record Snapshot(Map<Long, ThreadSnapshot> threads, int totalThreadCount) {
    }

    public record ThreadSnapshot(long id, String name, String state, boolean daemon) {
        static ThreadSnapshot from(final Thread thread, final ThreadInfo info) {
            final String state = info == null || info.getThreadState() == null
                ? thread.getState().name()
                : info.getThreadState().name();
            return new ThreadSnapshot(thread.getId(), thread.getName(), state, thread.isDaemon());
        }
    }

    public record LeakReport(List<ThreadSnapshot> leakedThreads, int totalThreadDelta) {
        public static LeakReport none() {
            return new LeakReport(List.of(), 0);
        }

        public boolean hasLeaks() {
            return leakedThreads != null && !leakedThreads.isEmpty();
        }

        public String summary() {
            if (!hasLeaks()) {
                return "nenhuma thread residual detectada";
            }
            return leakedThreads.stream()
                .map(thread -> thread.name() + "(id=" + thread.id() + ",state=" + thread.state() + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("thread leak detectado");
        }
    }
}
