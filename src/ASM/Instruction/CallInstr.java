package ASM.Instruction;

import ASM.Module.Block;

public class CallInstr extends Instruction {
    public String label;

    public CallInstr(Block parent, String label) {
        super(parent, "call");
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("%-8s%s", instr, label);
    }
}
