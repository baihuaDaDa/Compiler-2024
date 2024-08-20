package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IREntity.IREntity;
import Util.IREntity.IRLocalVar;

public class IcmpInstr extends Instruction {
    public String cond;
    public IRLocalVar result;
    public IREntity lhs, rhs;

    public IcmpInstr(IRBlock parent, String cond, IREntity lhs, IREntity rhs, IRLocalVar result) {
        super(parent);
        this.cond = cond;
        this.lhs = lhs;
        this.rhs = rhs;
        this.result = result;
    }

    @Override
    public String toString() {
        return result + " = icmp " + cond + " " + result.type + " " + lhs + ", " + rhs;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
