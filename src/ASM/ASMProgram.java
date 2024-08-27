package ASM;

import java.util.ArrayList;

public class ASMProgram {
    public ArrayList<ASMSection> sections;
    public int brCnt = 0, selectCnt = 0;

    public ASMProgram() {
        sections = new ArrayList<>();
    }

    public ASMSection getSection(String name) {
        for (ASMSection section : sections)
            if (section.name.equals(name))
                return section;
        ASMSection newSection = new ASMSection(name);
        sections.add(newSection);
        return newSection;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (ASMSection section : sections)
            ret.append(section.toString());
        return ret.toString();
    }
}
