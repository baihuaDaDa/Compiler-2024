package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRLocalVar;
import Util.IRObject.IREntity.IRVariable;

import java.util.HashMap;
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
    public void rename(HashMap<IRLocalVar, IREntity> renameMap) {
        if (value instanceof IRLocalVar localValue && renameMap.containsKey(localValue))
            value = renameMap.get(localValue);
        if (pointer instanceof IRLocalVar localPointer && renameMap.containsKey(localPointer))
            pointer = (IRVariable) renameMap.get(localPointer);
    }

    @Override
    public IRLocalVar getDef() {
        return null;
    }

    @Override
    public HashSet<IRLocalVar> getUse() {
        return new HashSet<>() {{
            if (value instanceof IRLocalVar localValue) add(localValue);
            if (pointer instanceof IRLocalVar localPointer) add(localPointer);
        }};
    }
}
