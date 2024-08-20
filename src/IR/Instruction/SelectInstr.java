package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IREntity.IREntity;
import Util.IREntity.IRLocalVar;

public class SelectInstr extends Instruction {
    public IRLocalVar result, cond;
    public IREntity lhs, rhs;

    public SelectInstr(IRBlock parent, IRLocalVar result, IRLocalVar cond, IREntity lhs, IREntity rhs) {
        super(parent);
        this.result = result;
        this.cond = cond;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public String toString() {
        return result + " = select i1 " + cond + ", " + lhs.type + " " + lhs + ", " + rhs.type + " " + rhs;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
