package ASM.Instruction;

import ASM.Module.Block;
import Util.PhysicalReg;

public class BinaryInstr extends Instruction {
    public PhysicalReg dst;
    public PhysicalReg src1;
    public PhysicalReg src2;

    public BinaryInstr(String instr, Block parent, PhysicalReg dst, PhysicalReg src1, PhysicalReg src2) {
        super(parent, instr);
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }

    public static String getInstr(String op) {
        return switch (op) {
            case "add", "sub", "mul", "and", "or", "xor" -> op;
            case "sdiv", "srem" -> op.substring(1);
            case "shl" -> "sll";
            case "ashr" -> "sra";
            default -> throw new RuntimeException("Unexpected operation: " + op);
        };
    }

    @Override
    public String toString() {
        return String.format("%-8s%s, %s, %s", instr, dst, src1, src2);
    }
}
