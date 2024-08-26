package ASM.Module;

import ASM.ASMSection;
import ASM.Instruction.Instruction;

import java.util.ArrayList;

public class Block {
    public FuncDefMod parent;
    public String label;
    public ArrayList<Instruction> body;

    public Block(FuncDefMod parent, String label) {
        this.parent = parent;
        this.label = label;
        body = new ArrayList<>();
    }

    public void addInstr(Instruction instr) {
        body.add(instr);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(label).append(":\n");
        for (Instruction instr : body)
            ret.append("\t").append(instr).append("\n");
        return ret.toString();
    }
}
