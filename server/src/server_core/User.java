package server_core;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class User {
    final String name;
    final AtomicLong credits = new AtomicLong(0);
    final AtomicLong creditsSpent = new AtomicLong(0);
    final AtomicLong runsCount = new AtomicLong(0);
    final AtomicInteger mainUploaded = new AtomicInteger(0);
    final AtomicInteger helperContrib = new AtomicInteger(0);
    volatile long lastSeenMs = System.currentTimeMillis();

    User(String name) { this.name = name; }
    public String name() { return name; }

    /** Current credits balance. */
    public long getCredits() {
        return credits.get();
    }

    /** Try to charge a positive amount; returns false and leaves balance unchanged if insufficient funds. */
    public boolean tryCharge(long amount) {
        long after = credits.addAndGet(-amount);
        if (after < 0) {
            // Roll back if balance would become negative
            credits.addAndGet(amount);
            return false;
        }
        return true;
    }

    /** Credit the account (positive amounts only). Returns the new balance. */
    public long addCredits(long amount) {
        if (amount <= 0) return credits.get();
        return credits.addAndGet(amount);
    }
}
