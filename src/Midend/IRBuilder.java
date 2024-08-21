package Midend;

import IR.IRBlock;
import IR.IRProgram;
import IR.IRVisitor;
import IR.Instruction.*;
import IR.Module.*;
import Util.Scope.GlobalScope;
import Util.Scope.IRScope;

public class IRBuilder implements IRVisitor {
    public GlobalScope gScope;
    public IRProgram program;
    public IRBlock curBlock;
    public IRScope curScope;

    public IRBuilder(GlobalScope gScope) {
        this.gScope = gScope;
        this.program = new IRProgram();
    }

    public void visit(IRProgram program) {}
    public void visit(IRBlock block) {}

    public void visit(AllocaInstr instr) {}
    public void visit(BinaryInstr instr) {}
    public void visit(BrInstr instr) {}
    public void visit(CallInstr instr) {}
    public void visit(GetelementptrInstr instr) {}
    public void visit(IcmpInstr instr) {}
    public void visit(LoadInstr instr) {}
    public void visit(PhiInstr instr) {}
    public void visit(RetInstr instr) {}
    public void visit(SelectInstr instr) {}
    public void visit(StoreInstr instr) {}

    public void visit(FuncDeclMod mod) {}
    public void visit(FuncDefMod mod) {}
    public void visit(GlobalVarDefMod mod) {}
    public void visit(StringLiteralDefMod mod) {}
    public void visit(StructDefMod mod) {}
}
