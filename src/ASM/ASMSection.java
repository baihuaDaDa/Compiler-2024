package ASM;

import ASM.Module.Module;

import java.util.ArrayList;

public class ASMSection {
    public String name;
    public ArrayList<Module> modules;

    public ASMSection(String name) {
        this.name = name;
        modules = new ArrayList<>();
    }

    public void addModule(Module block) {
        modules.add(block);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("\t.section ").append(name).append("\n");
        for (Module module : modules)
            ret.append(module);
        ret.append("\n");
        return ret.toString();
    }
}
