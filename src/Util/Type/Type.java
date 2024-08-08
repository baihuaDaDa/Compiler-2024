package Util.Type;

public class Type extends BaseType {
    public int dim = 0;

    public Type(String typename, int dim) {
        super(typename);
        this.dim = dim;
    }

    public Type(BaseType other) {
        super(other);
        if (other instanceof Type)
            this.dim = ((Type) other).dim;
    }

    @Override
    public boolean isSameType(BaseType other) {
        Type otherType = new Type(other);
        return super.isSameType(other) && this.dim == otherType.dim;
    }
}
