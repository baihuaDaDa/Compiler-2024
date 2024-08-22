package Util.IRObject.IREntity;

import AST.Literal.LiteralNode;
import IR.Instruction.Instruction;
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

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
