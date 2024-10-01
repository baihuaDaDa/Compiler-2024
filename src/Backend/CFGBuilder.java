package Backend;

import ASM.ASMProgram;
import ASM.ASMSection;
import ASM.Instruction.BnezInstr;
import ASM.Module.Block;
import ASM.Module.FuncDefMod;
import ASM.Module.Module;

public class CFGBuilder {
    private final ASMProgram program;

    public CFGBuilder(ASMProgram program) {
        this.program = program;
    }

    public void build() {
        for (ASMSection section : program.sections)
            if (section.name.equals(".text")) section.modules.forEach(this::buildFunc);
    }

    public void buildFunc(Module func) {
        if (!(func instanceof FuncDefMod)) throw new RuntimeException("Non-function module in section .text");
        ((FuncDefMod) func).body.forEach(this::buildBlock);
    }

    public void buildBlock(Block block) {
        for (var instr : block.body) {
            if (instr instanceof BnezInstr bnezInstr) {
                block.suc.add(getTarget(bnezInstr, block.parent));
                bnezInstr.target.pred.add(block);
            }
        }
    }

    private Block getTarget(BnezInstr instr, FuncDefMod parent) {
        if (instr.target != null) return instr.target;
        for (var block : parent.body)
            if (block.label.equals(instr.label)) {
                instr.target = block;
                return block;
            }
        throw new RuntimeException("Undefined label in branch instruction");
    }
}
