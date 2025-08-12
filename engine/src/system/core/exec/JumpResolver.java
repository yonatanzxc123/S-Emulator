package system.core.exec;

public interface JumpResolver {
    /** @return instruction index to jump to (0-based), or -1 to signal EXIT */
    int resolve(String label);
}
