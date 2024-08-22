package Util.IRObject.IREntity;

import Util.Type.IRType;

public class IRGlobalPtr extends IRVariable {
    public IRGlobalPtr(String name, IRType type) {
        super(type, name);
    }

    @Override
    public String toString() {
        return "@" + name;
    }
}
