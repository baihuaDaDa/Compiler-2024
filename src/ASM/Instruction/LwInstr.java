package ASM.Instruction;

import ASM.Module.Block;
import ASM.Operand.PhysicalReg;

public class LwInstr extends Instruction {
    public PhysicalReg dst;
    public int offset;
    public PhysicalReg base;

    public LwInstr(Block parent, PhysicalReg dst, int offset, PhysicalReg base) {
        super(parent, "lw");
        this.dst = dst;
        this.offset = offset;
        this.base = base;
    }

    @Override

    public String toString() {
        return String.format("%-8s%s, %d(%s)", instr, dst, offset, base);
    }
}
