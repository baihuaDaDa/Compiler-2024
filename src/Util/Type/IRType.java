package Util.Type;

public class IRType {
    public boolean isVoid = false, isInt = false, isPtr = false;
    public int bitSize = 0;

    public IRType(String type) {
        switch (type) {
            case "void" -> isVoid = true;
            case "ptr" -> isPtr = true;
            default -> {
                if (!type.startsWith("i"))
                    throw new RuntimeException("Unknown type: " + type);
                isInt = true;
                bitSize = Integer.parseInt(type.substring(1));
            }
        }
    }

    @Override
    public String toString() {
        if (isVoid) return "void";
        if (isPtr) return "ptr";
        if (isInt) return "i" + Integer.toString(bitSize);
        throw new RuntimeException("Unprintable type");
    }

    public boolean isSameType(IRType other) {
        if (isVoid && other.isVoid) return true;
        if (isPtr && other.isPtr) return true;
        if (isInt && other.isInt) return bitSize == other.bitSize;
        return false;
    }
}
