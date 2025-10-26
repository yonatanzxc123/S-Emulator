package server_core;

import java.util.ArrayList;
import java.util.List;
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

    private final List<RunRecord> runHistory = new ArrayList<>();

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

    public synchronized void addRunRecord(RunRecord record) {
        runHistory.add(record);
    }

    public synchronized List<RunRecord> getRunHistory() {
        return new ArrayList<>(runHistory);
    }

    public static class RunRecord {
        public final long runNo;
        public final boolean isMainProgram;
        public final String name;
        public final String arch;
        public final int degree;
        public final long y;
        public final long cycles;
        public final List<Long> inputs;

        public RunRecord(long runNo, boolean isMainProgram, String name, String arch, int degree, long y, long cycles, List<Long> inputs) {
            this.runNo = runNo;
            this.isMainProgram = isMainProgram;
            this.name = name;
            this.arch = arch;
            this.degree = degree;
            this.y = y;
            this.cycles = cycles;
            this.inputs = inputs;
        }
    }
}
