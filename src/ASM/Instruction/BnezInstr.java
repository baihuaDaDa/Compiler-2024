package ASM.Instruction;

import ASM.Module.Block;
import ASM.Operand.PhysicalReg;

public class BnezInstr extends Instruction {
    public PhysicalReg src;
    public String label; // label与当前的PC的offset不能超过12位
    public Block target;

    public BnezInstr(Block parent, PhysicalReg src, String label) {
        super(parent, "bnez");
        this.src = src;
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("%-8s%s, %s", instr, src, label);
    }
}
