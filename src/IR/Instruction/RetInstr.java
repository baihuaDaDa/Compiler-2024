package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IREntity;

public class RetInstr extends Instruction {
    public IREntity value;

    public RetInstr(IRBlock parent, IREntity value) {
        super(parent);
        this.value = value;
    }

    @Override
    public String toString() {
        if (value == null) return "ret void";
        return "ret " + value.type + " " + value;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
