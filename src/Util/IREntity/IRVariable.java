package Util.IREntity;

import Util.Type.IRType;

abstract public class IRVariable extends IREntity {
    public String name;

    public IRVariable(IRType type, String name) {
        super(type);
        this.name = name;
    }

    @Override
    abstract public String toString();
}
