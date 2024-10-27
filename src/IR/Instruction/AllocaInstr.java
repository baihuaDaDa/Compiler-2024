package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRLocalVar;
import Util.Type.IRType;

import java.util.HashMap;
import java.util.HashSet;

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

    @Override
    public void rename(HashMap<IRLocalVar, IREntity> renameMap) {}

    @Override
    public IRLocalVar getDef() {
        throw new RuntimeException("AllocaInstr.getDef() should not be called");
    }

    @Override
    public HashSet<IRLocalVar> getUse() {
        throw new RuntimeException("AllocaInstr.getUse() should not be called");
    }
}
