package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IRLocalVar;

import java.util.HashSet;

abstract public class Instruction {
    public IRBlock parent;

    public Instruction(IRBlock parent) {
        this.parent = parent;
    }

    @Override
    abstract public String toString();

    abstract public void accept(IRVisitor visitor);

    abstract public IRLocalVar getDef();

    abstract public HashSet<IRLocalVar> getUse();
}
