package ASM.Instruction;

import ASM.Module.Block;
import ASM.Operand.PhysicalReg;

public class SetInstr extends Instruction {
    public PhysicalReg dst;
    public PhysicalReg src;

    public SetInstr(String instr, Block parent, PhysicalReg dst, PhysicalReg src) {
        super(parent, instr);
        this.dst = dst;
        this.src = src;
    }

    @Override
    public String toString() {
        return String.format("%-8s%s, %s", instr, dst, src);
    }
}
