package IR;

import IR.Instruction.Instruction;
import IR.Module.FuncDefMod;

import java.util.ArrayList;

public class IRBlock {
    public ArrayList<Instruction> instructions = null;
    public FuncDefMod parent;

    public IRBlock(FuncDefMod parent) {
        this.parent = parent;
        this.instructions = new ArrayList<>();
    }

    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
