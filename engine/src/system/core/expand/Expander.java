package system.core.expand;

import system.core.model.Program;

public interface Expander {
    /** degree=0 => original program; degree>0 => apply expansion 'degree' times */
    Program expandToDegree(Program program, int degree);
}

