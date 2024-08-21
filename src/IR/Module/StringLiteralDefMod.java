package IR.Module;

import IR.IRVisitor;
import Util.IREntity.IRGlobalPtr;
import Util.Type.IRType;

public class StringLiteralDefMod extends Module {
    public String value;
    public IRGlobalPtr ptr;

    public StringLiteralDefMod(String value, IRGlobalPtr ptr) {
        this.value = value;
        this.ptr = ptr;
    }

    @Override
    public String toString() {
        return ptr + " = private unnamed_addr constant [" + (value.length() + 1) +  " x i8]" + " c\"" + value + "\"";
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}