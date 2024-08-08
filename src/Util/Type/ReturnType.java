package Util.Type;

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

    @Override
    public boolean isSameType(BaseType other) {
        if (other instanceof ExprType) {
            if (((ExprType) other).isNull) {
                return dim > 0 || isClass;
            }
        }
        ReturnType otherType = new ReturnType(other);
        if (isVoid && otherType.isVoid)
            return true;
        if (!isVoid && !otherType.isVoid)
            return super.isSameType(other);
        return false;
    }
}
