package Util.Type;

import Util.Decl.FuncDecl;

public class ExprType extends ReturnType {
    public boolean isNull = false;
    public boolean isArbitrary = false;
    public boolean isFunc = false;
    public FuncDecl funcDecl;

    public ExprType(BaseType other) {
        super(other);
        if (other instanceof ExprType) {
            isNull = ((ExprType) other).isNull;
            isArbitrary = ((ExprType) other).isArbitrary;
            isFunc = ((ExprType) other).isFunc;
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

    public ExprType(String funcName, FuncDecl funcDecl) {
        super(funcName, 0);
        isClass = false;
        isFunc = true;
        this.funcDecl = funcDecl;
    }

    @Override
    public boolean isSameType(BaseType other) {
        ExprType otherType = new ExprType(other);
        if (otherType.isFunc || isFunc) return false;
        if (otherType.isNull) return dim > 0 || isClass || isNull;
        if (isNull) return otherType.dim > 0 || otherType.isClass;
        if ((otherType.isArbitrary || isArbitrary) && !otherType.isVoid && !isVoid)
            // 空数组可以兼容不小于自己维度的数组
            return (otherType.isArbitrary && otherType.dim <= dim) || (isArbitrary && dim <= otherType.dim);
        return super.isSameType(other);
    }

    @Override
    public String toString() {
        if (isNull) return "null";
        else if (isArbitrary) return "{".repeat(dim) + "}".repeat(dim);
        else return super.toString();
    }
}
