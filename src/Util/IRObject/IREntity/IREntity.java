package Util.IRObject.IREntity;

import Util.Type.IRType;

abstract public class IREntity {
    public IRType type;

    public IREntity(IRType type) {
        this.type = type;
    }

    @Override
    abstract public String toString();
}
