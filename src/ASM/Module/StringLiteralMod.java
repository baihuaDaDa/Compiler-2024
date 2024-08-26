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
        return String.format("%s:\n\t%-8s%s\n\t%-8s%s, %d", label, ".asciz", str, ".size", label, size);
    }
}
