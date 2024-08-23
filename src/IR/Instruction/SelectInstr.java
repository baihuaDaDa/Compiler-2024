package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRLocalVar;

public class SelectInstr extends Instruction {
    public IRLocalVar result;
    public IREntity cond, lhs, rhs;

    public SelectInstr(IRBlock parent, IRLocalVar result, IREntity cond, IREntity lhs, IREntity rhs) {
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
