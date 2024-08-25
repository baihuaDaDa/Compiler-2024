package IR.Module;

import IR.IRVisitor;
import Util.IRObject.IREntity.IRGlobalPtr;

public class StringLiteralDefMod extends Module {
    public String value;
    public IRGlobalPtr ptr;

    public StringLiteralDefMod(String value, IRGlobalPtr ptr) {
        this.value = value;
        this.ptr = ptr;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(ptr).append(" = private unnamed_addr constant [").append(value.length() + 1).append(" x i8] c\"");
        for (int i = 0; i < value.length(); i++) {
            switch (value.charAt(i)) {
                case '\n' -> ret.append("\\0A");
                case '\"' -> ret.append("\\22");
                case '\\' -> ret.append("\\\\");
                default -> ret.append(value.charAt(i));
            }
        }
        ret.append("\\00\"");
        return ret.toString();
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}