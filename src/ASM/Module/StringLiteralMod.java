package ASM.Module;

import ASM.ASMSection;

public class StringLiteralMod extends Module {
    public String str;
    public int size;

    public StringLiteralMod(ASMSection parent, String label, String str, int size) {
        super(parent, label);
        this.str = str;
        this.size = size;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            switch (str.charAt(i)) {
                case '\n' -> ret.append("\\n");
                case '\"' -> ret.append("\\\"");
                case '\\' -> ret.append("\\\\");
                default -> ret.append(str.charAt(i));
            }
        }
        return String.format("%s:\n\t%-8s\"%s\"\n\t%-8s%s, %d\n", label, ".asciz", ret, ".size", label, size);
    }
}
