package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;

abstract public class Instruction {
    public IRBlock parent;

    public Instruction(IRBlock parent) {
        this.parent = parent;
    }

    @Override
    abstract public String toString();

    abstract public void accept(IRVisitor visitor);
}
