package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRLocalVar;
import Util.IRObject.IREntity.IRVariable;

import java.util.HashMap;
import java.util.HashSet;

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

    @Override
    public void rename(HashMap<IRLocalVar, IREntity> renameMap) {
        if (pointer instanceof IRLocalVar localPointer && renameMap.containsKey(localPointer))
            pointer = (IRVariable) renameMap.get(localPointer);
    }

    @Override
    public IRLocalVar getDef() {
        return result;
    }

    @Override
    public HashSet<IRLocalVar> getUse() {
        return new HashSet<>() {{
            if (pointer instanceof IRLocalVar localPointer) add(localPointer);
        }};
    }
}
