package ASM.Module;

import ASM.ASMSection;
import ASM.Instruction.Instruction;

import java.util.ArrayList;
import java.util.HashSet;

public class Block {
    public FuncDefMod parent;
    public String label;
    public ArrayList<Instruction> body;

    // CFG
    public HashSet<Block> pred;
    public HashSet<Block> suc;

    // Live Analysis

    public Block(FuncDefMod parent, String label) {
        this.parent = parent;
        this.label = label;
        body = new ArrayList<>();
        pred = new HashSet<>();
        suc = new HashSet<>();
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
