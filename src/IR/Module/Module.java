package IR.Module;

import IR.IRVisitor;

abstract public class Module {
    @Override
    abstract public String toString();

    abstract public void accept(IRVisitor visitor);
}
