package ASM.Instruction;

import ASM.Module.Block;
import ASM.Operand.PhysicalReg;

public class BinaryImmInstr extends Instruction {
    public PhysicalReg dst;
    public PhysicalReg src;
    public int imm;

    public BinaryImmInstr(String instr, Block parent, PhysicalReg dst, PhysicalReg src, int imm) {
        super(parent, instr);
        this.dst = dst;
        this.src = src;
        this.imm = imm;
    }

    @Override

    public String toString() {
        return String.format("%-8s%s, %s, %d", instr, dst, src, imm);
    }
}
