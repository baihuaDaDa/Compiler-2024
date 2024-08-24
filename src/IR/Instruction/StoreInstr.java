package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRVariable;

public class StoreInstr extends Instruction {
    public IREntity value;
    public IRVariable pointer;

    public StoreInstr(IRBlock parent, IREntity value, IRVariable pointer) {
        super(parent);
        this.value = value;
        this.pointer = pointer;
    }

    @Override
    public String toString() {
        return "store " + value.type + " " + value + ", ptr " + pointer;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
