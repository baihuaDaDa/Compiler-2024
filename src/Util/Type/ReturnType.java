package Util.Type;

import Parser.MxParser;

public class ReturnType extends Type {
    public boolean isVoid = false;

    public ReturnType(BaseType other) {
        super(other);
        if (other instanceof ReturnType)
            isVoid = ((ReturnType) other).isVoid;
    }

    public ReturnType(String typename, int dim) {
        super(typename, dim);
        if (typename.equals("void")) {
            isClass = false;
            isVoid = true;
        }
    }

    public ReturnType(MxParser.ReturnTypeContext ctx) {
        this(ctx.type() == null ? "void" : ctx.type().baseType().getText(),
                ctx.type() == null ? 0 : ctx.type().LeftBracket().size());
    }

    @Override
    public boolean isSameType(BaseType other) {
        if (other instanceof ExprType otherType) {
            if (otherType.isFunc) return false;
            if (otherType.isNull) return dim > 0 || isClass;
            if (otherType.isArbitrary && !isVoid) return otherType.dim <= dim;
        }
        ReturnType otherType = new ReturnType(other);
        if (isVoid && otherType.isVoid)
            return true;
        if (!isVoid && !otherType.isVoid)
            return super.isSameType(other);
        return false;
    }

    @Override
    public String toString() {
        return isVoid ? "void" : super.toString();
    }
}
