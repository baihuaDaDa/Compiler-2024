package ASM.Module;

import ASM.ASMSection;
import ASM.Instruction.BnezInstr;
import ASM.Instruction.Instruction;
import ASM.Instruction.JInstr;

import java.util.ArrayList;
import java.util.HashSet;

public class Block {
    public FuncDefMod parent;
    public String label;
    public ArrayList<Instruction> body;

    // Live Analysis

    public Block(FuncDefMod parent, String label) {
        this.parent = parent;
        this.label = label;
        body = new ArrayList<>();
    }

    public void addInstr(Instruction instr) {
        body.add(instr);
    }

    public void addInstrBeforeJump(Instruction instr) {
        for (int i = body.size() - 1; i >= 0; i--)
            if (!(body.get(i) instanceof JInstr) || !(body.get(i) instanceof BnezInstr))
                body.add(i + 1, instr);
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
