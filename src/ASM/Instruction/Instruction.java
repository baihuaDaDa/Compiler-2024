package ASM.Instruction;

import ASM.Module.Block;

abstract public class Instruction {
    public Block parent;
    public String instr;

    public Instruction(Block parent, String instr) {
        this.parent = parent;
        this.instr = instr;
    }

    @Override
    abstract public String toString();
}
