package ASM.Instruction;

import ASM.Module.Block;
import Util.PhysicalReg;

public class LiInstr extends Instruction {
    public PhysicalReg dst;
    public int imm; // pseudo -> allow 32-bit imm

    public LiInstr(Block parent, PhysicalReg dst, int imm) {
        super(parent, "li");
        this.dst = dst;
        this.imm = imm;
    }

    @Override
    public String toString() {
        return String.format("%-8s%s, %s", instr, dst, imm);
    }
}
