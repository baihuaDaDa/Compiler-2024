package Util.Type;

import Parser.MxParser;

public class BaseType {
    public boolean isInt = false, isBool = false, isString = false, isClass = false;
    public String baseTypename;

    public BaseType(String typename) {
        this.baseTypename = typename;
        switch (typename) {
            case "int": isInt = true; break;
            case "bool": isBool = true; break;
            case "string": isString = true; break;
            default: isClass = true;
        }
    }

    public BaseType(BaseType other) {
        isInt = other.isInt;
        isBool = other.isBool;
        isString = other.isString;
        isClass = other.isClass;
        baseTypename = other.baseTypename;
    }

    public BaseType(MxParser.BaseTypeContext ctx) {
        this(ctx.getText());
    }

    public boolean isSameType(BaseType other) {
        return (isInt && other.isInt) || (isBool && other.isBool) || (isString && other.isString)
                || (isClass && other.isClass && baseTypename.equals(other.baseTypename));
    }

    @Override
    public String toString() {
        return baseTypename;
    }
}