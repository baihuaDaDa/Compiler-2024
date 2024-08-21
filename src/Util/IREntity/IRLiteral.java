package Util.IREntity;

import Util.Type.IRType;

public class IRLiteral extends IREntity {
    public String value; // 1 for ture; 0 for false

    public IRLiteral(IRType type, String value) {
        super(type);
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
