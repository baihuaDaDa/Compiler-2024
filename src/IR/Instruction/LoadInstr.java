package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IREntity.IRLocalVar;
import Util.IREntity.IRVariable;
import Util.Type.IRType;

public class LoadInstr extends Instruction {
    public IRLocalVar result;
    public IRVariable pointer;

    public LoadInstr(IRBlock parent, IRLocalVar result, IRVariable pointer) {
        super(parent);
        this.result = result;
        this.pointer = pointer;
    }

    @Override
    public String toString() {
        return result + " = load " + result.type + ", ptr " + pointer;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
