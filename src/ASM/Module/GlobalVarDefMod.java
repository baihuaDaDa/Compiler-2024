package ASM.Module;

import ASM.ASMSection;

public class GlobalVarDefMod extends Module {
    public int num;

    public GlobalVarDefMod(ASMSection parent, String label, int num) {
        super(parent, label);
        this.num = num;
    }

    @Override
    public String toString() {
        return String.format("\t%-8s%s\n%s:\n\t%-8s%d\n", ".globl", label, label, ".word", num);
    }
}
