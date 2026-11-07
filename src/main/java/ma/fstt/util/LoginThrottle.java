package ma.fstt.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Slows down password guessing: after {@code max} failures inside the time
 * window, further attempts for the same key are refused until the oldest
 * failure leaves the window. The key is "source address + e-mail", so one
 * attacker cannot lock a victim out from elsewhere.
 *
 * In memory and per instance: enough for a single Tomcat, reset on restart.
 */
public final class LoginThrottle {

    private static final int MAX_KEYS = 10_000;

    private final int max;
    private final long fenetreMs;
    private final LongSupplier horloge;
    private final Map<String, Deque<Long>> echecs = new HashMap<>();

    public LoginThrottle(int max, long fenetreMs) {
        this(max, fenetreMs, System::currentTimeMillis);
    }

    /** The clock is injectable so the behaviour can be checked without waiting. */
    public LoginThrottle(int max, long fenetreMs, LongSupplier horloge) {
        this.max = max;
        this.fenetreMs = fenetreMs;
        this.horloge = horloge;
    }

    /** Seconds to wait before the next attempt is accepted; 0 means "go ahead". */
    public synchronized long secondesRestantes(String cle) {
        Deque<Long> file = echecs.get(cle);
        if (file == null) {
            return 0;
        }
        long maintenant = horloge.getAsLong();
        purger(file, maintenant);
        if (file.isEmpty()) {
            echecs.remove(cle);
            return 0;
        }
        if (file.size() < max) {
            return 0;
        }
        long attenteMs = file.peekFirst() + fenetreMs - maintenant;
        return Math.max(1, (attenteMs + 999) / 1000);
    }

    public synchronized void echec(String cle) {
        if (echecs.size() > MAX_KEYS) {
            echecs.clear(); // crude memory bound: better to forget than to grow forever
        }
        echecs.computeIfAbsent(cle, k -> new ArrayDeque<>()).addLast(horloge.getAsLong());
    }

    public synchronized void reussite(String cle) {
        echecs.remove(cle);
    }

    private void purger(Deque<Long> file, long maintenant) {
        while (!file.isEmpty() && maintenant - file.peekFirst() >= fenetreMs) {
            file.removeFirst();
        }
    }
}
