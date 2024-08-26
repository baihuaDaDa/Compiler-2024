package ASM.Instruction;

import ASM.Module.Block;
import ASM.Operand.PhysicalReg;

public class BTypeInstr extends Instruction {
    public PhysicalReg src1;
    public PhysicalReg src2;
    public String label; // label与当前的PC的offset不能超过12位

    public BTypeInstr(String instr, Block parent, PhysicalReg src1, PhysicalReg src2, String label) {
        super(parent, instr);
        this.src1 = src1;
        this.src2 = src2;
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("%-8s%s, %s, %s", instr, src1, src2, label);
    }
}
