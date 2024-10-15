package ASM.Instruction;

import ASM.Module.Block;
import Util.PhysicalReg;

public class LaInstr extends Instruction {
    public PhysicalReg dst;
    public String label;

    public LaInstr(Block parent, PhysicalReg dst, String label) {
        super(parent, "la");
        this.dst = dst;
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("%-8s%s, %s", instr, dst, label);
    }
}
