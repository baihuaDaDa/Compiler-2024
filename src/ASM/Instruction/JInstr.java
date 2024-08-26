package ASM.Instruction;

import ASM.Module.Block;

public class JInstr extends Instruction {
    public String label; // label与当前PC的offset不能超过20位

    public JInstr(Block parent, String label) {
        super(parent, "j");
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("%-8s%s", instr, label);
    }
}
