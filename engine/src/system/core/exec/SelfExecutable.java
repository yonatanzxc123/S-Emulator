package system.core.exec;

import system.core.model.Instruction;

public interface SelfExecutable {
    /**
     * Execute this instruction on the machine state, including
     * updating cycles exactly according to the instruction's spec.
     * Implement this on any new instruction to avoid touching Executor.
     */
    void executeSelf(MachineState s, JumpResolver j);
}
