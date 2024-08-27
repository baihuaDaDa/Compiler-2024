package ASM;

import java.util.ArrayList;

public class ASMProgram {
    public ArrayList<ASMSection> sections;
    public int brCnt = 0, selectCnt = 0;

    public ASMProgram() {
        sections = new ArrayList<>();
        sections.add(new ASMSection("text"));
        sections.add(new ASMSection("data"));
        sections.add(new ASMSection("rodata"));
    }

    public ASMSection getSection(String name) {
        for (ASMSection section : sections)
            if (section.name.equals(name))
                return section;
        return null;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (ASMSection section : sections)
            ret.append(section.toString());
        return ret.toString();
    }
}
