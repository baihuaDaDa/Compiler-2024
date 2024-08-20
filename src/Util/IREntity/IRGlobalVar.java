package Util.IREntity;

import Util.Type.IRType;

public class IRGlobalVar extends IRVariable {
    public IRGlobalVar(String name, IRType type) {
        super(type, name);
    }

    @Override
    public String toString() {
        return "@" + name;
    }
}
