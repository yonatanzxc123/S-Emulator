package system.api;

import java.util.List;
import java.io.Serializable;
import java.util.Map;

public record HistoryEntry(int runNo, int degree, List<Long> inputs, long y, long cycles, Map<String, Long> finalVariables) implements Serializable{}
