package ASM.Instruction;

import ASM.Module.Block;

public class CommentInstr extends Instruction {
    public String comment;

    public CommentInstr(Block parent, String comment) {
        super(parent, "#");
        this.comment = comment;
    }

    @Override
    public String toString() {
        return instr + " " + comment;
    }
}
