package system.core.exec;

import system.core.model.Program;
import java.util.Map;
import java.util.concurrent.Callable;
// this class holds the function definitions for the current execution context
// it is stored in a ThreadLocal so that it can be accessed from anywhere
// without having to pass it around explicitly
// it is set by the Executor when it starts executing a program
// and cleared when the execution is finished
// this allows functions to be called from anywhere in the code
// without having to pass the function definitions around explicitly
// this is especially useful for instructions that need to call functions
// such as Call and Return
// it also allows for nested function calls
// without having to worry about passing the function definitions around
// this is a simple implementation that uses a ThreadLocal to store the function definitions
// it is not the most efficient way to do this, but it is simple and works well
// it is also thread-safe, as each thread has its own ThreadLocal
// this means that multiple threads can execute programs with different function definitions
// without interfering with each other
// this is important for a multi-threaded environment
// such as a web server or a game engine wich will probaly done in part 3
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

    // pretty symbol for a function, falling back to the formal name (meaning userString and program name from the xml)
    public String prettyNameOf(String functionName) {
        Program p = functions.get(functionName);
        if (p == null) return functionName;
        String u = p.userString();
        return (u == null || u.isBlank()) ? functionName : u;
    }





}
