package Backend;

import ASM.ASMProgram;
import ASM.Instruction.*;
import ASM.Module.Block;
import ASM.Operand.PhysicalReg;
import IR.IRBlock;
import IR.IRProgram;
import IR.IRVisitor;
import IR.Instruction.*;
import IR.Instruction.BinaryInstr;
import IR.Instruction.CallInstr;
import IR.Module.*;
import Util.IRObject.IREntity.IRLiteral;
import Util.IRObject.IREntity.IRLocalVar;

public class ASMBuilder implements IRVisitor {
    ASMProgram program;
    Block curBlock;

    public ASMBuilder() {
        this.program = new ASMProgram();
    }

    public void visit(IRProgram program) {
        program.accept(this);
    }
    public void visit(IRBlock block) {
        if (!block.label.equals("entry")) {
            Block asmBlock = new Block(curBlock.parent, block.label);
            curBlock.parent.addBlock(asmBlock);
            curBlock = asmBlock;
        }
        for (var instr : block.instructions)
            instr.accept(this);
    }

    public void visit(AllocaInstr instr) {}
    public void visit(IR.Instruction.BinaryInstr instr) {}
    public void visit(BrInstr instr) {
        if (instr.cond != null) {
            int offset = curBlock.parent.getVirtualReg(instr.cond.name);
            curBlock.addInstr(new LwInstr(curBlock, PhysicalReg.get("t0"), offset, PhysicalReg.get("sp")));
            curBlock.addInstr(new BnezInstr(curBlock, PhysicalReg.get("t0")));
            curBlock.addInstr(new JInstr(curBlock, ));
            curBlock.addInstr(new JInstr(curBlock,));
        } else curBlock.addInstr(new JInstr(curBlock, ));
    }
    public void visit(IR.Instruction.CallInstr instr) {}
    public void visit(GetelementptrInstr instr) {}
    public void visit(IcmpInstr instr) {}
    public void visit(LoadInstr instr) {}
    public void visit(PhiInstr instr) {}
    public void visit(IR.Instruction.RetInstr instr) {
        if (instr.value != null) {
            if (instr.value instanceof IRLiteral) curBlock.addInstr(new LiInstr(curBlock, PhysicalReg.get("a0"), ((IRLiteral) instr.value).value));
            else if (instr.value instanceof IRLocalVar) {
                int offset = curBlock.parent.getVirtualReg(((IRLocalVar) instr.value).name);
                curBlock.addInstr(new LwInstr(curBlock, PhysicalReg.get("a0"), offset, PhysicalReg.get("sp")));
            }
        }
        curBlock.addInstr(new LwInstr(curBlock, PhysicalReg.get("ra"), curBlock.parent.getRetReg(), PhysicalReg.get("sp")));
        curBlock.addInstr(new BinaryImmInstr("addi", curBlock, PhysicalReg.get("sp"), PhysicalReg.get("sp"), curBlock.parent.stackSize));
        curBlock.addInstr(new ASM.Instruction.RetInstr(curBlock));
    }
    public void visit(SelectInstr instr) {}
    public void visit(StoreInstr instr) {
    }

    public void visit(FuncDeclMod mod) {}
    public void visit(IR.Module.FuncDefMod mod) {
        var curSection = program.getSection("text");
        var newFunc = new ASM.Module.FuncDefMod(curSection, mod.funcName);
        curSection.addModule(newFunc);
        curBlock = newFunc.body.getFirst();
        for (var block : mod.body)
            for  (var instr : block.instructions) {
                if (instr instanceof AllocaInstr) newFunc.allocPtrMap.put(((AllocaInstr) instr).result.name, newFunc.allocCnt++);
                else {
                    if (instr instanceof LoadInstr) newFunc.virtualRegMap.put(((LoadInstr) instr).result.name, newFunc.virtualRegCnt++);
                    if (instr instanceof BinaryInstr) newFunc.virtualRegMap.put(((BinaryInstr) instr).result.name, newFunc.virtualRegCnt++);
                    if (instr instanceof GetelementptrInstr) newFunc.virtualRegMap.put(((GetelementptrInstr) instr).result.name, newFunc.virtualRegCnt++);
                    if (instr instanceof CallInstr)
                        if (((CallInstr) instr).result != null) newFunc.virtualRegMap.put(((CallInstr) instr).result.name, newFunc.virtualRegCnt++);
                    if (instr instanceof IcmpInstr) newFunc.virtualRegMap.put(((IcmpInstr) instr).result.name, newFunc.virtualRegCnt++);
                    if (instr instanceof SelectInstr) newFunc.virtualRegMap.put(((SelectInstr) instr).result.name, newFunc.virtualRegCnt++);
                }
                if (instr instanceof CallInstr) newFunc.argCnt = Math.max(newFunc.argCnt, ((CallInstr) instr).args.size());
            }
        newFunc.stackSize = (newFunc.argCnt + newFunc.virtualRegCnt + newFunc.allocCnt + 1) * 4;
        if (newFunc.stackSize % 16 != 0) newFunc.stackSize = ((newFunc.stackSize) / 16 + 1) * 16;
    }
    public void visit(IR.Module.GlobalVarDefMod mod) {
        var curSection = program.getSection("data");
        curSection.addModule(new ASM.Module.GlobalVarDefMod(curSection, mod.globalVar.name, mod.init.isNull ? 0 : mod.init.value));
    }
    public void visit(IR.Module.StringLiteralDefMod mod) {
        var curSection = program.getSection("rodata");
        curSection.addModule(new ASM.Module.StringLiteralMod(curSection, mod.ptr.name, mod.value, mod.value.length() + 1));
    }
    public void visit(StructDefMod mod) {}
}
