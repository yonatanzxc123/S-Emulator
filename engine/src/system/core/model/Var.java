package system.core.model;

public record Var(char kind, int index) {
    // kind: 'x', 'z', or 'y' (y ignores index)
    public String asText() {
        return (kind == 'y') ? "y" : (kind + String.valueOf(index));
    }
    public static Var x(int i) { return new Var('x', i); }
    public static Var z(int i) { return new Var('z', i); }
    public static Var y()      { return new Var('y', 0); }
}
