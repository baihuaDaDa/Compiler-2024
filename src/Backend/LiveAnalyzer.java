package Backend;

import IR.IRBlock;
import IR.IRProgram;
import IR.Instruction.BrInstr;
import IR.Instruction.RetInstr;
import IR.Module.FuncDefMod;

import java.util.ArrayList;
import java.util.HashSet;

public class LiveAnalyzer {
    private final IRProgram program;

    public LiveAnalyzer(IRProgram program) {
        this.program = program;
    }

    public void analyze() {
        program.funcDefs.forEach(this::analyzeFunc);
        if (program.initFunc != null) analyzeFunc(program.initFunc);
        analyzeFunc(program.mainFunc);
    }

    public void analyzeFunc(FuncDefMod func) {
        GetUseDef(func);
        boolean[] visited = new boolean[func.body.size()];
        ArrayList<IRBlock> ord = new ArrayList<>();
        Traverse(ord, visited, func.body.getFirst());
        boolean converge = false;
        while (!converge) {
            converge = true;
            for (int i = ord.size() - 1; i >= 0; --i) {
                var instructions = func.body.get(i).instructions;
                for (int j = instructions.size() - 1; j >= 0; --j) {
                    var instr = instructions.get(j);
                    var defs = program.defMap.get(instr);
                    var outs = program.outMap.get(instr);
                    var ins = program.inMap.get(instr);
                    // scan_liveOut
                    if (j != instructions.size() - 1) {
                        var tmpIns = program.inMap.get(instructions.get(j + 1));
                        if (tmpIns == null) continue;
                        for (var in : tmpIns) {
                            if (outs.contains(in)) continue;
                            outs.add(in);
                            converge = false;
                        }
                    } else {
                        if (instr instanceof RetInstr) continue;
                        if (instr instanceof BrInstr brInstr) {
                            var tmpIns = program.inMap.get(brInstr.thenBlock.instructions.getFirst());
                            if (tmpIns == null) continue;
                            for (var in : tmpIns) {
                                if (outs.contains(in)) continue;
                                outs.add(in);
                                converge = false;
                            }
                            if (brInstr.elseBlock != null) {
                                tmpIns = program.inMap.get(brInstr.elseBlock.instructions.getFirst());
                                if (tmpIns == null) continue;
                                for (var in : tmpIns) {
                                    if (outs.contains(in)) continue;
                                    outs.add(in);
                                    converge = false;
                                }
                            }
                        } else throw new RuntimeException("Non-jump exit instruction in block");
                    }
                    // scan_liveIn
                    for (var out : outs) {
                        if (defs.contains(out)) continue;
                        if (ins.contains(out)) continue;
                        ins.add(out);
                        converge = false;
                    }
                }
            }
        }
    }

    private void GetUseDef(FuncDefMod func) {
        for (var block : func.body)
            for (var instr : block.instructions) {
                program.useMap.put(instr, instr.getUse());
                program.defMap.put(instr, instr.getDef());
                program.outMap.put(instr, new HashSet<>());
                program.inMap.put(instr, new HashSet<>(program.useMap.get(instr)));
            }
    }

    // Inverse Post Order
    private void Traverse(ArrayList<IRBlock> ord, boolean[] visited, IRBlock block) {
        visited[block.blockNo] = true;
        for (var suc : block.suc)
            if (!visited[suc.blockNo]) Traverse(ord, visited, suc);
        ord.add(block);
    }
}