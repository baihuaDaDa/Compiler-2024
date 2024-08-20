package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IREntity.IREntity;
import Util.IREntity.IRLocalVar;
import Util.Type.IRType;

public class BinaryInstr extends Instruction {
    public String op;
    public IREntity lhs, rhs;
    public IRLocalVar result;

    public BinaryInstr(IRBlock parent, String op, IREntity lhs, IREntity rhs, IRLocalVar result) {
        super(parent);
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
        this.result = result;
    }

    @Override
    public String toString() {
        return result + " = " + op + " " + result.type + " " + lhs + ", " + rhs;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
