package system.api.view;

public record CommandView(
        int number,            // 1-based index
        boolean basic,         // true=B, false=S
        String labelOrEmpty,   // "" or "L3" or "EXIT"
        String text,           // e.g., "IF x1 != 0 GOTO L34" or "x2 <- x2 - 1"
        int cycles,            // cycles count
        String originChain     // "" for now; later used by Expand to show "<<< ..."
) {}
