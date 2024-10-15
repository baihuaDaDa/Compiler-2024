package ASM.Instruction;

import ASM.Module.Block;
import Util.PhysicalReg;

public class MvInstr extends Instruction {
    public PhysicalReg dst;
    public PhysicalReg src;

    public MvInstr(Block parent, PhysicalReg dst, PhysicalReg src) {
        super(parent, "mv");
        this.dst = dst;
        this.src = src;
    }

    @Override
    public String toString() {
        return String.format("%-8s%s, %s", instr, dst, src);
    }
}
