package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRLocalVar;
import Util.IRObject.IREntity.IRVariable;

import java.util.HashSet;

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

    @Override
    public HashSet<IRLocalVar> getDef() {
        return new HashSet<>();
    }

    @Override
    public HashSet<IRLocalVar> getUse() {
        return new HashSet<>() {{
            if (value instanceof IRLocalVar localValue) add(localValue);
            if (pointer instanceof IRLocalVar localPointer) add(localPointer);
        }};
    }
}
