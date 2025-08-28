package system.api;

import java.util.List;
import java.io.Serializable;

public record HistoryEntry(int runNo, int degree, List<Long> inputs, long y, long cycles) implements Serializable{}
