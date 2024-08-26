package ASM;

import java.util.ArrayList;

public class ASMProgram {
    public ArrayList<ASMSection> sections;

    public ASMProgram() {
        sections = new ArrayList<>();
    }

    public void addSection(ASMSection section) {
        sections.add(section);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (ASMSection section : sections)
            ret.append(section.toString());
        return ret.toString();
    }
}
