package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IREntity.IREntity;
import Util.IREntity.IRLocalVar;
import Util.Type.IRType;

public class AllocaInstr extends Instruction {
    public IRLocalVar result;
    public IRType type;

    public AllocaInstr(IRBlock parent, IRLocalVar result, IRType type) {
        super(parent);
        this.result = result;
        this.type = type;
    }

    @Override
    public String toString() {
        return result + " = alloca " + type;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
