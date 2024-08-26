package ASM.Instruction;

import ASM.Module.Block;
import ASM.Operand.PhysicalReg;

public class SwInstr extends Instruction {
    public PhysicalReg src;
    public int offset;
    public PhysicalReg base;

    public SwInstr(Block parent, PhysicalReg src, int offset, PhysicalReg base) {
        super(parent, "sw");
        this.src = src;
        this.offset = offset;
        this.base = base;
    }

    @Override
    public String toString() {
        return String.format("%-8s%s, %d(%s)", instr, src, offset, base);
    }
}
