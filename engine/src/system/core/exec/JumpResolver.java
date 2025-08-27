package system.core.exec;

public interface JumpResolver {
    /** @return instruction index to jump to (0-based),-1 to signal NOT_FOUND or -2 for EXIT */
        int NOT_FOUND = -1;
        int EXIT = -2;
        int resolve(String label);

}
