package system.core.model;
import java.io.Serializable;


public final class Var implements Serializable  {
    public enum Type { X, Z, Y }   // the variable family we use Enum here cuz its small

    private final Type type;       // X, Z, or Y
    private final int index;       // X/Z: 1.. ; Y: 0

    private Var(Type type, int index) {
        this.type = type;
        this.index = index;
    }

    public static Var x(int i) { if (i < 1) throw new IllegalArgumentException("x index >= 1"); return new Var(Type.X, i); }
    public static Var z(int i) { if (i < 1) throw new IllegalArgumentException("z index >= 1"); return new Var(Type.Z, i); }
    public static Var y()      { return new Var(Type.Y, 0); }

    public Type type() { return type; }
    public int index() { return index; }

    public boolean isX() { return type == Type.X; }
    public boolean isZ() { return type == Type.Z; }
    public boolean isY() { return type == Type.Y; }

    /** For display: 'x', 'z', or 'y'. (we can change in the future for more characters */
    public char symbol() { return type == Type.X ? 'x' : (type == Type.Z ? 'z' : 'y'); }

    @Override public String toString() {
        return isY() ? "y" : (symbol() + Integer.toString(index));
    }
}
