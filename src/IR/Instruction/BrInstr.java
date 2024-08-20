package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IREntity.IRLocalVar;

public class BrInstr extends Instruction {
    public IRLocalVar cond;
    public IRBlock thenBlock, elseBlock;

    public BrInstr(IRBlock parent, IRLocalVar cond, IRBlock thenBlock, IRBlock elseBlock) {
        super(parent);
        this.cond = cond;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    @Override
    public String toString() {
        if (cond != null) return "br i1 " + cond + ", label " + thenBlock + ", " + elseBlock;
        return "br label " + thenBlock;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
