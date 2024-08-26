package ASM.Instruction;

import ASM.Module.Block;
import ASM.Operand.PhysicalReg;

public class LiInstr extends Instruction {
    public PhysicalReg dst;
    public String imm; // pseudo -> allow 32-bit imm

    public LiInstr(Block parent, PhysicalReg dst, String imm) {
        super(parent, "li");
        this.dst = dst;
        this.imm = imm;
    }

    @Override
    public String toString() {
        return String.format("%-8s%s, %s", instr, dst, imm);
    }
}
