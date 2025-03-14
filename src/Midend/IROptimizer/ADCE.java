package Midend.IROptimizer;

import IR.IRBlock;
import IR.IRProgram;
import IR.Instruction.*;
import IR.Module.FuncDefMod;
import Midend.IROptimizer.Util.CDGBuilder;
import Midend.IROptimizer.Util.IRCFGBuilder;
import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;
import java.util.HashSet;

public class ADCE {
    private final IRProgram program;
    private final IRCFGBuilder irCFGBuilder;

    public ADCE(IRProgram program) {
        this.program = program;
        this.irCFGBuilder = new IRCFGBuilder(program);
    }

    public void run() {
        CDGBuilder CDGBuilder = new CDGBuilder(program);
        CDGBuilder.build();
        program.funcDefs.forEach(this::runFunc);
        if (program.initFunc != null) runFunc(program.initFunc);
        runFunc(program.mainFunc);
    }

    private void runFunc(FuncDefMod func) {
        HashSet<Instruction> liveInstr = new HashSet<>();
        HashSet<IRBlock> liveBlock = new HashSet<>();
        ArrayList<Instruction> workList = new ArrayList<>();
        // init
        for (var block : func.body) {
            for (var phiInstr : block.phiInstrs.values()) func.defInstrMap.put(phiInstr.getDef(), phiInstr);
            for (var instr : block.instructions) {
                func.defInstrMap.put(instr.getDef(), instr);
                if (instr instanceof CallInstr || instr instanceof StoreInstr || instr instanceof RetInstr)
                    workList.add(instr);
            }
        }
        // iterate
        while (!workList.isEmpty()) {
            var instr = workList.getFirst();
            workList.remove(instr);
            if (instr instanceof PhiInstr phiInstr)
                for (var pair : phiInstr.pairs)
                    if (!liveInstr.contains(pair.b.instructions.getLast()))
                        workList.add(pair.b.instructions.getLast());
            for (var cdgPred : instr.parent.cdgDomFrontier)
                if (!liveInstr.contains(cdgPred.instructions.getLast()))
                    workList.add(cdgPred.instructions.getLast());
            for (var use : instr.getUse()) {
                Instruction defInstr = instr.parent.parent.defInstrMap.get(use);
                if (defInstr != null && !liveInstr.contains(defInstr)) workList.add(defInstr);
            }
            liveInstr.add(instr);
            liveBlock.add(instr.parent);
        }
        // remove dead code
        for (var block : func.body) {
            HashSet<String> phiInstrsToRemove = new HashSet<>();
            HashSet<Instruction> instrsToRemove = new HashSet<>();
            for (var entry : block.phiInstrs.entrySet())
                if (!liveInstr.contains(entry.getValue())) phiInstrsToRemove.add(entry.getKey());
            for (var instr : block.instructions)
                if (!liveInstr.contains(instr)) instrsToRemove.add(instr);
            block.phiInstrs.keySet().removeAll(phiInstrsToRemove);
            block.instructions.removeAll(instrsToRemove);
            // 如果块的后继不活跃，那么找到的第一个活跃的后继就是新的后继
            // 只需一直迭代 anti_dom
            if (block.instructions.isEmpty() || !(block.instructions.getLast() instanceof BrInstr || block.instructions.getLast() instanceof RetInstr)) {
                IRBlock newSuc = block.cdgFather;
                while (!liveBlock.contains(newSuc) && newSuc != newSuc.cdgFather) newSuc = newSuc.cdgFather;
                block.instructions.addLast(new BrInstr(block, null, newSuc, null));
            }
        }
        irCFGBuilder.clearFunc(func);
        irCFGBuilder.buildFunc(func);
        HashSet<IRBlock> removeBBList = new HashSet<>();
        for (var block : func.body)
            if (!liveBlock.contains(block)) {
                if (block.suc.size() != 1) throw new RuntimeException("ADCE: deadBlock.suc.size() != 1");
                IRBlock suc = block.suc.iterator().next();
                for (var pred : block.pred) {
                    BrInstr terminal = (BrInstr) pred.instructions.getLast();
                    if (terminal.thenBlock == block) terminal.thenBlock = suc;
                    if (terminal.elseBlock == block) terminal.elseBlock = suc;
                    pred.suc.remove(block);
                    pred.suc.add(suc);
                }
                for (var phiInstr : suc.phiInstrs.values())
                    for (var pair : phiInstr.pairs)
                        if (pair.b == block) {
                            phiInstr.pairs.remove(pair);
                            phiInstr.pairs.add(new Pair<>(pair.a, suc));
                            break;
                        }
                suc.pred.remove(block);
                removeBBList.add(block);
            }
        func.body.removeAll(removeBBList);
    }
}
