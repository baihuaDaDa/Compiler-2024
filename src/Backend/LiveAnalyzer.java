package Backend;

import IR.IRBlock;
import IR.IRProgram;
import IR.Instruction.BrInstr;
import IR.Instruction.Instruction;
import IR.Instruction.RetInstr;
import IR.Module.FuncDefMod;

import java.util.ArrayList;
import java.util.HashMap;
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
        // get uses and defs
        GetUseDef(func);
        // post traverse
        boolean[] visited = new boolean[func.body.size()];
        ArrayList<IRBlock> ord = new ArrayList<>();
        Traverse(ord, visited, func.body.getFirst());
        // get linear order
        HashMap<Instruction, Integer> linearOrderMap = new HashMap<>();
        GetLinearOrder(linearOrderMap, ord);
        // get live ins and live outs
        boolean converge = false;
        while (!converge) {
            converge = true;
            // TODO 可以把 ret 的基本块入队，用反向 BFS 遍历
            for (int i = ord.size() - 1; i >= 0; --i) {
                var instructions = func.body.get(i).instructions;
                for (int j = instructions.size() - 1; j >= 0; --j) {
                    var instr = instructions.get(j);
                    var defs = func.defMap.get(instr);
                    var outs = func.outMap.get(instr);
                    var ins = func.inMap.get(instr);
                    // scan_liveOut
                    if (j != instructions.size() - 1) {
                        var tmpIns = func.inMap.get(instructions.get(j + 1));
                        if (tmpIns == null) continue;
                        for (var in : tmpIns) {
                            if (outs.contains(in)) continue;
                            outs.add(in);
                            converge = false;
                        }
                    } else {
                        if (instr instanceof RetInstr) continue;
                        if (instr instanceof BrInstr brInstr) {
                            var tmpIns = func.inMap.get(brInstr.thenBlock.instructions.getFirst());
                            if (tmpIns == null) continue;
                            for (var in : tmpIns) {
                                if (outs.contains(in)) continue;
                                outs.add(in);
                                converge = false;
                            }
                            if (brInstr.elseBlock != null) {
                                tmpIns = func.inMap.get(brInstr.elseBlock.instructions.getFirst());
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
        // get live intervals
        // TODO 死代码块消除
        for (var block : func.body) {
            for (var instr : block.instructions) {
                var ins = func.inMap.get(instr);
                var linearOrder = linearOrderMap.get(instr);
                for (var in : ins) {
                    if (func.intervalMap.containsKey(in))
                        func.intervalMap.get(in).end = Math.max(linearOrder, func.intervalMap.get(in).end);
                    else func.intervalMap.put(in, new FuncDefMod.Interval(linearOrder, linearOrder));
                }
                var outs = func.outMap.get(instr);
                for (var out : outs) {
                    if (func.intervalMap.containsKey(out))
                        func.intervalMap.get(out).start = Math.min(linearOrder, func.intervalMap.get(out).start);
                    else func.intervalMap.put(out, new FuncDefMod.Interval(linearOrder, linearOrder));
                }
            }
        }
    }

    private void GetUseDef(FuncDefMod func) {
        for (var block : func.body)
            for (var instr : block.instructions) {
                func.useMap.put(instr, instr.getUse());
                func.defMap.put(instr, instr.getDef());
                func.outMap.put(instr, new HashSet<>());
                func.inMap.put(instr, new HashSet<>(func.useMap.get(instr)));
            }
    }

    // Inverse Post Order
    private void Traverse(ArrayList<IRBlock> ord, boolean[] visited, IRBlock block) {
        visited[block.blockNo] = true;
        for (var suc : block.suc)
            if (!visited[suc.blockNo]) Traverse(ord, visited, suc);
        ord.add(block);
    }

    private void GetLinearOrder(HashMap<Instruction, Integer> linearOrderMap, ArrayList<IRBlock> ord) {
        int linearOrder = 0;
        for (int i = ord.size() - 1; i >= 0; --i) {
            var block = ord.get(i);
            for (var instr : block.instructions)
                linearOrderMap.put(instr, linearOrder++);
        }
    }
}