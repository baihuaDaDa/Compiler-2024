package IR;

import IR.Instruction.BrInstr;
import IR.Instruction.Instruction;
import IR.Instruction.RetInstr;
import IR.Module.FuncDefMod;

import java.util.ArrayList;
import java.util.HashSet;

public class IRBlock {
    public String label;
    public ArrayList<Instruction> instructions;
    public FuncDefMod parent;

    // CFG
    public HashSet<IRBlock> pred = new HashSet<>();
    public HashSet<IRBlock> suc = new HashSet<>();

    public IRBlock(FuncDefMod parent, String label) {
        this.parent = parent;
        this.label = label;
        this.instructions = new ArrayList<>();
    }

    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(label).append(":\n");
        for (Instruction instr : instructions)
            ret.append("  ").append(instr).append("\n");
        ret.append("\n");
        return ret.toString();
    }

    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }

    public void addInstr(Instruction instr) {
        instructions.add(instr);
    }

    public void addInstr(int index, Instruction instr) {
        instructions.add(index, instr);
    }
}
