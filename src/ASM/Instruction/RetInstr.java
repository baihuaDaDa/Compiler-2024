package ASM.Instruction;

import ASM.Module.Block;

public class RetInstr extends Instruction {

    public RetInstr(Block parent) {
        super(parent, "ret");
    }

    @Override

    public String toString() {
        return instr;
    }
}
