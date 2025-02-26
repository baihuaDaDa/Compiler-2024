package IR;

import ASM.Module.Block;
import IR.Instruction.Instruction;
import IR.Instruction.PhiInstr;
import IR.Module.FuncDefMod;
import Util.IRObject.IREntity.IRLocalVar;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;

public class IRBlock {
    public String label;
    public HashMap<String, PhiInstr> phiInstrs;
    public ArrayList<Instruction> instructions;
    public FuncDefMod parent;

    // CFG
    public HashSet<IRBlock> pred;
    public HashSet<IRBlock> suc;

    // Dominator Tree
    public int blockNo;
    public BitSet dom;
    public IRBlock idom;
    public HashSet<IRBlock> children;
    public HashSet<IRBlock> domFrontier;

    public IRBlock cdgIdom;
    public HashSet<IRBlock> cdgDomFrontier;

    // SSA Solver
    public Block asmBlock;

    public IRBlock(FuncDefMod parent, String label) {
        this.parent = parent;
        this.label = label;
        this.phiInstrs = new HashMap<>();
        this.instructions = new ArrayList<>();
        this.pred = new HashSet<>();
        this.suc = new HashSet<>();
        this.children = new HashSet<>();
        this.domFrontier = new HashSet<>();
    }

    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(label).append(":\n");
        for (PhiInstr instr : phiInstrs.values())
            ret.append("  ").append(instr).append("\n");
        for (Instruction instr : instructions)
            ret.append("  ").append(instr).append("\n");
        ret.append("\n");
        return ret.toString();
    }

    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }

    public void addPhiInstr(PhiInstr instr) {
        phiInstrs.put(instr.result.name, instr);
    }

    public void addInstr(Instruction instr) {
        instructions.add(instr);
    }

    public void addInstr(int index, Instruction instr) {
        instructions.add(index, instr);
    }
}
