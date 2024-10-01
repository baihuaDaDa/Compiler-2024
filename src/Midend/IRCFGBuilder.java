package Midend;

import IR.IRBlock;
import IR.IRProgram;
import IR.Instruction.BrInstr;
import IR.Module.FuncDefMod;

public class IRCFGBuilder {
    private final IRProgram program;

    public IRCFGBuilder(IRProgram program) {
        this.program = program;
    }

    public void build() {
        program.funcDefs.forEach(this::buildFunc);
    }

    public void buildFunc(FuncDefMod func) {
        for (int i = 0; i < func.body.size(); ++i) {
            var block = func.body.get(i);
            block.blockNo = i;
            buildBlock(block);
        }
    }

    public void buildBlock(IRBlock block) {
        for (var instr : block.instructions) {
            if (instr instanceof BrInstr brInstr) {
                block.suc.add(brInstr.thenBlock);
                brInstr.thenBlock.pred.add(block);
                if (brInstr.elseBlock != null) {
                    block.suc.add(brInstr.elseBlock);
                    brInstr.elseBlock.pred.add(block);
                }
            }
        }
    }
}