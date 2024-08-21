package IR;

import IR.Instruction.*;
import IR.Module.*;

public interface IRVisitor {
    void visit(IRProgram program);
    void visit(IRBlock block);

    void visit(AllocaInstr instr);
    void visit(BinaryInstr instr);
    void visit(BrInstr instr);
    void visit(CallInstr instr);
    void visit(GetelementptrInstr instr);
    void visit(IcmpInstr instr);
    void visit(LoadInstr instr);
    void visit(PhiInstr instr);
    void visit(RetInstr instr);
    void visit(SelectInstr instr);
    void visit(StoreInstr instr);

    void visit(FuncDeclMod mod);
    void visit(FuncDefMod mod);
    void visit(GlobalVarDefMod mod);
    void visit(StringLiteralDefMod mod);
    void visit(StructDefMod mod);
}
