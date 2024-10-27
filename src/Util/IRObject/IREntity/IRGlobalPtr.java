package Util.IRObject.IREntity;

import Util.Type.IRType;

public class IRGlobalPtr extends IRVariable {
    public IRType objectType = null;

    public IRGlobalPtr(String name, IRType type, IRType objectType) {
        super(type, name);
        this.objectType = objectType;
    }

    @Override
    public String toString() {
        return "@" + name;
    }
}
