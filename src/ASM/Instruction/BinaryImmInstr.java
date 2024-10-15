package ASM.Instruction;

import ASM.Module.Block;
import Util.PhysicalReg;

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

    public static String getInstr(String op) {
        return switch (op) {
            case "add", "and", "or", "xor" -> op + "i";
            case "shl" -> "slli";
            case "ashr" -> "srai";
            default -> throw new RuntimeException("Unexpected operation: " + op);
        };
    }

    @Override
    public String toString() {
        return String.format("%-8s%s, %s, %d", instr, dst, src, imm);
    }
}
