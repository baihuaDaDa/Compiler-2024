package ASM.Module;

import ASM.ASMSection;
import ASM.Instruction.Instruction;

import java.util.ArrayList;

abstract public class Module {
    public ASMSection parent;
    public String label;

    public Module(ASMSection parent, String label) {
        this.parent = parent;
        this.label = label;
    }

    @Override
    abstract public String toString();
}
