package system.api;

import java.util.List;

public record HistoryEntry(int runNo, int degree, List<Long> inputs, long y, long cycles) {}
