package AST.Type;

public class BaseType {
    private boolean isInt = false, isBool = false, isString = false, isClass = false;
    private String classname;

    public BaseType(String typename) {
        classname = typename;
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
    }
}