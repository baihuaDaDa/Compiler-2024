package Midend.IROptimizer.Util;

import IR.IRBlock;
import IR.IRProgram;
import IR.Instruction.BrInstr;
import IR.Instruction.RetInstr;
import IR.Module.FuncDefMod;

import java.util.HashSet;

public class IRCFGBuilder {
    private final IRProgram program;

    public IRCFGBuilder(IRProgram program) {
        this.program = program;
    }

    public void build() {
        clear();
        program.funcDefs.forEach(this::buildFunc);
        if (program.initFunc != null) buildFunc(program.initFunc);
        buildFunc(program.mainFunc);
    }

    private void clear() {
        program.funcDefs.forEach(this::clearFunc);
        if (program.initFunc != null) clearFunc(program.initFunc);
        clearFunc(program.mainFunc);
    }

    private void clearFunc(FuncDefMod func) {
        for (var block : func.body) {
            block.suc = new HashSet<>();
            block.pred = new HashSet<>();
        }
    }

    private void buildFunc(FuncDefMod func) {
        HashSet<IRBlock> visited = new HashSet<>();
        buildBlock(func.body.getFirst(), visited);
        // 此处直接删除了控制流图上没有的基本块，记得删除phi语句里对应的前驱
        HashSet<IRBlock> removeList = new HashSet<>();
        for (var block : func.body)
            if (!visited.contains(block)) {
                if (block.instructions.getLast() instanceof BrInstr terminal) {
                    for (var phiInstr : terminal.thenBlock.phiInstrs.values())
                        phiInstr.pairs.removeIf(pair -> pair.b == block);
                    if (terminal.elseBlock != null)
                        for (var phiInstr : terminal.elseBlock.phiInstrs.values())
                            phiInstr.pairs.removeIf(pair -> pair.b == block);
                }
                removeList.add(block);
            }
        func.body.removeAll(removeList);
        for (int i = 0; i < func.body.size(); ++i) {
            var block = func.body.get(i);
            block.blockNo = i;
        }
    }

    private void buildBlock(IRBlock block, HashSet<IRBlock> visited) {
        visited.add(block);
        var ctrlInstr = block.instructions.getLast();
        if (ctrlInstr instanceof RetInstr) return;
        if (ctrlInstr instanceof BrInstr brInstr) {
            block.suc.add(brInstr.thenBlock);
            brInstr.thenBlock.pred.add(block);
            if (!visited.contains(brInstr.thenBlock)) buildBlock(brInstr.thenBlock, visited);
            if (brInstr.elseBlock != null) {
                block.suc.add(brInstr.elseBlock);
                brInstr.elseBlock.pred.add(block);
                if (!visited.contains(brInstr.elseBlock)) buildBlock(brInstr.elseBlock, visited);
            }
        } else throw new RuntimeException("Unknown control instruction");
    }
}