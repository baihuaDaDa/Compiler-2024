package Util.Type;

public class ExprType extends ReturnType {
    public boolean isNull = false;
    public boolean isArbitrary = false;

    public ExprType(BaseType other) {
        super(other);
        if (other instanceof ExprType) {
            isNull = ((ExprType) other).isNull;
            isArbitrary = ((ExprType) other).isArbitrary;
        }
    }

    public ExprType(String typename, int dim) {
        super(typename, dim);
        if (typename.equals("null")) {
            isNull = true;
            isClass = false;
        } else if (typename.isEmpty()) {
            isArbitrary = true;
            isClass = false;
        }
    }

    @Override
    public boolean isSameType(BaseType other) {
        ExprType otherType = new ExprType(other);
        if (otherType.isNull) return dim > 0 || isClass;
        if (isNull) return otherType.dim > 0 || otherType.isClass;
        if (otherType.isArbitrary || isArbitrary)
            // 空数组可以兼容不小于自己维度的数组
            return (otherType.isArbitrary && otherType.dim <= dim) || (isArbitrary && dim <= otherType.dim);
        return super.isSameType(other);
    }

    @Override
    public String toString() {
        return isNull ? "null" : super.toString();
    }
}
