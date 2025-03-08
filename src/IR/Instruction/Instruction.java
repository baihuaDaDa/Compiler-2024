package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRLocalVar;

import java.util.HashMap;
import java.util.HashSet;

abstract public class Instruction {
    public IRBlock parent;

    public Instruction(IRBlock parent) {
        this.parent = parent;
    }

    public boolean isPinned() {
        return !(this instanceof BinaryInstr) && !(this instanceof IcmpInstr) && !(this instanceof GetelementptrInstr);
    }

    @Override
    abstract public String toString();

    abstract public void accept(IRVisitor visitor);

    abstract public void rename(HashMap<IRLocalVar, IREntity> renameMap);

    abstract public IRLocalVar getDef();

    abstract public HashSet<IRLocalVar> getUse();
}
