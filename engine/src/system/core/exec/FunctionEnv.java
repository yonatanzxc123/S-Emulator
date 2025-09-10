package system.core.exec;

import system.core.model.Program;
import java.util.Map;
import java.util.concurrent.Callable;

public final class FunctionEnv {
    private static final ThreadLocal<FunctionEnv> TL = new ThreadLocal<>();
    private final Map<String, Program> functions;

    public FunctionEnv(Map<String,Program> f) { this.functions = Map.copyOf(f); }
    public Program get(String name) { return functions.get(name); }

    public static FunctionEnv current() {
        var e = TL.get();
        if (e == null) throw new IllegalStateException("FunctionEnv not set");
        return e;
    }

    public static <T> T with(FunctionEnv env, Callable<T> body) {
        var prev = TL.get();
        TL.set(env);
        try { return body.call(); }
        catch (RuntimeException | Error re) { throw re; }
        catch (Exception e) { throw new RuntimeException(e); }
        finally { TL.set(prev); }
    }
}
