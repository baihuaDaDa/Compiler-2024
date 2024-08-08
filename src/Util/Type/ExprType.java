package Util.Type;

public class ExprType extends ReturnType {
    public boolean isNull = false;

    public ExprType(BaseType other) {
        super(other);
        if (other instanceof ExprType)
            isNull = ((ExprType) other).isNull;
    }

    public ExprType(String typename, int dim) {
        super(typename, dim);
        if (typename.equals("null")) {
            isNull = true;
            isClass = false;
        }
    }

    @Override
    public boolean isSameType(BaseType other) {
        ExprType otherType = new ExprType(other);
        if (otherType.isNull) return dim > 0 || isClass;
        if (isNull) return otherType.dim > 0 || otherType.isClass;
        return super.isSameType(other);
    }
}
