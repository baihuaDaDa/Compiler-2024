package Util.IRObject.IREntity;

import Util.Type.IRType;

public class IRLocalVar extends IRVariable {
    public IRLocalVar(String name, IRType type) {
        super(type, name);
    }

    @Override
    public String toString() {
        return "%" + name;
    }
}
