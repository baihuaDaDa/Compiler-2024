package Midend;

import IR.IRBlock;
import IR.IRProgram;
import IR.Instruction.BrInstr;
import IR.Instruction.RetInstr;
import IR.Module.FuncDefMod;

import java.util.ArrayList;
import java.util.HashSet;

public class IRCFGBuilder {
    private final IRProgram program;

    public IRCFGBuilder(IRProgram program) {
        this.program = program;
    }

    public void build() {
        program.funcDefs.forEach(this::buildFunc);
        if (program.initFunc != null) buildFunc(program.initFunc);
        buildFunc(program.mainFunc);
    }

    public void buildFunc(FuncDefMod func) {
        HashSet<IRBlock> visited = new HashSet<>();
        buildBlock(func.body.getFirst(), visited);
        // 此处直接删除了控制流图上没有的基本块
        func.body.removeIf(block -> !visited.contains(block));
        for (int i = 0; i < func.body.size(); ++i) {
            var block = func.body.get(i);
            block.blockNo = i;
        }
    }

    public void buildBlock(IRBlock block, HashSet<IRBlock> visited) {
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