package Util.IRObject.IREntity;

import AST.Literal.LiteralNode;
import Util.Type.IRType;

public class IRLiteral extends IREntity {
    public int value; // 1 for ture; 0 for false
    public boolean isNull = false;

    public IRLiteral(IRType type, int value) {
        super(type);
        this.value = value;
    }

    public IRLiteral(IRType type, boolean isNull) {
        super(type);
        this.isNull = isNull;
    }

    public IRLiteral(LiteralNode node) {
        super(new IRType(node.type));
        if (node.type.isNull) isNull = true;
        else if (node.type.isBool) value = node.constLogic ? 1 : 0;
        else value = node.constInt;
    }

    @Override
    public String toString() {
        return isNull ? "null" : Integer.toString(value);
    }
}
