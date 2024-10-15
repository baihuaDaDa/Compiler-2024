package Backend;

import ASM.ASMProgram;
import ASM.Instruction.*;
import ASM.Module.Block;
import Util.PhysicalReg;
import IR.IRBlock;
import IR.IRProgram;
import IR.IRVisitor;
import IR.Instruction.*;
import IR.Instruction.BinaryInstr;
import IR.Instruction.CallInstr;
import IR.Module.*;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRGlobalPtr;
import Util.IRObject.IREntity.IRLiteral;
import Util.IRObject.IREntity.IRLocalVar;

public class ASMBuilder implements IRVisitor {
    public ASMProgram program;
    Block curBlock;

    public ASMBuilder() {
        this.program = new ASMProgram();
    }

    public void visit(IRProgram program) {
        for (var func : program.funcDefs) func.accept(this);
        if (program.initFunc != null) program.initFunc.accept(this);
        program.mainFunc.accept(this);
        for (var globalVar : program.globalVarDefs) globalVar.accept(this);
        for (var stringLiteral : program.stringLiteralDefs) stringLiteral.accept(this);
    }
    public void visit(IRBlock block) {
        if (!block.label.equals("entry")) {
            Block asmBlock = new Block(curBlock.parent, block.label);
            curBlock.parent.addBlock(asmBlock);
            curBlock = asmBlock;
            block.asmBlock = asmBlock;
        }
        for (var instr : block.instructions)
            instr.accept(this);
    }

    // for lw: reg1 -> dst, reg2 -> base, imm -> offset
    // for sw: reg1 -> src, reg2 -> base, imm -> offset
    // for *i: reg1 -> dst, reg2 -> src, imm -> imm
    private void addInstrWithOverflowedImm(String instr, PhysicalReg reg1, int imm, PhysicalReg reg2) {
        if (imm < -2048 || imm > 2047) {
            // $t6 始终被占用了！！！
            curBlock.addInstr(new LiInstr(curBlock, PhysicalReg.get("t6"), imm));
            switch (instr) {
                case "lw" -> {
                    curBlock.addInstr(new ASM.Instruction.BinaryInstr("add", curBlock, PhysicalReg.get("t6"), PhysicalReg.get("t6"), reg2));
                    curBlock.addInstr(new LwInstr(curBlock, reg1, 0, PhysicalReg.get("t6")));
                }
                case "sw" -> {
                    curBlock.addInstr(new ASM.Instruction.BinaryInstr("add", curBlock, PhysicalReg.get("t6"), PhysicalReg.get("t6"), reg2));
                    curBlock.addInstr(new SwInstr(curBlock, reg1, 0, PhysicalReg.get("t6")));
                }
                default -> curBlock.addInstr(new ASM.Instruction.BinaryInstr(instr, curBlock, reg1, reg2, PhysicalReg.get("t6")));
            }
        } else switch (instr) {
            case "lw" -> curBlock.addInstr(new LwInstr(curBlock, reg1, imm, reg2));
            case "sw" -> curBlock.addInstr(new SwInstr(curBlock, reg1, imm, reg2));
            default -> curBlock.addInstr(new ASM.Instruction.BinaryImmInstr(instr + "i", curBlock, reg1, reg2, imm));
        }
    }
    private PhysicalReg loadReg(IREntity entity, PhysicalReg dst) {
        if (entity instanceof IRLiteral) {
            curBlock.addInstr(new LiInstr(curBlock, dst, ((IRLiteral) entity).value));
            return dst;
        } else if (entity instanceof IRLocalVar) {
            if (curBlock.parent.isVirtualReg(((IRLocalVar) entity).name)) {
                int offset = curBlock.parent.getVirtualReg(((IRLocalVar) entity).name);
                addInstrWithOverflowedImm("lw", dst, offset, PhysicalReg.get("sp"));
                return dst;
            } else if (curBlock.parent.isParamReg(((IRLocalVar) entity).name)) {
                int index = curBlock.parent.paramMap.get(((IRLocalVar) entity).name);
                if (index < 8) return PhysicalReg.get("a" + index);
                else {
                    int offset = curBlock.parent.getParamReg(((IRLocalVar) entity).name);
                    addInstrWithOverflowedImm("lw", dst, offset, PhysicalReg.get("sp"));
                    return dst;
                }
            }
        } else if (entity instanceof IRGlobalPtr) {
            curBlock.addInstr(new LaInstr(curBlock, dst, ((IRGlobalPtr) entity).name));
            return dst;
        }
        return null;
    }

    public void visit(AllocaInstr instr) {}
    public void visit(IR.Instruction.BinaryInstr instr) {
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        int offset = curBlock.parent.getVirtualReg(instr.result.name);
        loadReg(instr.lhs, PhysicalReg.get("t0"));
        loadReg(instr.rhs, PhysicalReg.get("t1"));
        curBlock.addInstr(new ASM.Instruction.BinaryInstr(ASM.Instruction.BinaryInstr.getInstr(instr.op), curBlock, PhysicalReg.get("t2"), PhysicalReg.get("t0"), PhysicalReg.get("t1")));
        addInstrWithOverflowedImm("sw", PhysicalReg.get("t2"), offset, PhysicalReg.get("sp"));
    }
    public void visit(BrInstr instr) {
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        if (instr.cond != null) {
            loadReg(instr.cond, PhysicalReg.get("t0"));
            Block brTure = new Block(curBlock.parent, String.format("br_true.%d", program.brCnt++));
            curBlock.addInstr(new BnezInstr(curBlock, PhysicalReg.get("t0"), brTure.label));
            curBlock.addInstr(new JInstr(curBlock, instr.elseBlock.label));
            curBlock.parent.addBlock(brTure);
            curBlock = brTure;
            curBlock.addInstr(new JInstr(curBlock, instr.thenBlock.label));
        } else curBlock.addInstr(new JInstr(curBlock, instr.thenBlock.label));
    }
    public void visit(IR.Instruction.CallInstr instr) {
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        int offset;
        for (int i = 0; i < Math.min(8, instr.args.size()); i++) {
            offset = curBlock.parent.getArgReg(i);
            addInstrWithOverflowedImm("sw", PhysicalReg.get("a" + i), offset, PhysicalReg.get("sp"));
        }
        for (int i = 0; i < instr.args.size(); i++) {
            loadReg(instr.args.get(i), PhysicalReg.get("t0"));
            if (i < 8) curBlock.addInstr(new MvInstr(curBlock, PhysicalReg.get("a" + i), PhysicalReg.get("t0")));
            else {
                offset = curBlock.parent.getArgReg(i);
                addInstrWithOverflowedImm("sw", PhysicalReg.get("t0"), offset, PhysicalReg.get("sp"));
            }
        }
        curBlock.addInstr(new ASM.Instruction.CallInstr(curBlock, instr.funcName));
        if (instr.result != null) {
            offset = curBlock.parent.getVirtualReg(instr.result.name);
            addInstrWithOverflowedImm("sw", PhysicalReg.get("a0"), offset, PhysicalReg.get("sp"));
        }
        for (int i = 0; i < Math.min(8, instr.args.size()); i++) {
            offset = curBlock.parent.getArgReg(i);
            addInstrWithOverflowedImm("lw", PhysicalReg.get("a" + i), offset, PhysicalReg.get("sp"));
        }
    }
    public void visit(GetelementptrInstr instr) {
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        curBlock.addInstr(new LiInstr(curBlock, PhysicalReg.get("t0"), 4)); // load size
        loadReg(instr.indices.size() == 1 ? instr.indices.getFirst() : instr.indices.get(1), PhysicalReg.get("t1")); // load index
        curBlock.addInstr(new ASM.Instruction.BinaryInstr("mul", curBlock, PhysicalReg.get("t1"), PhysicalReg.get("t0"), PhysicalReg.get("t1")));
        if (loadReg(instr.pointer, PhysicalReg.get("t0")) == null) {
            int offsetPtr = curBlock.parent.getAllocPtr(instr.pointer.name);
            curBlock.addInstr(new LiInstr(curBlock, PhysicalReg.get("t0"), offsetPtr));
            curBlock.addInstr(new ASM.Instruction.BinaryInstr("add", curBlock, PhysicalReg.get("t0"), PhysicalReg.get("t0"), PhysicalReg.get("sp")));
        }
        curBlock.addInstr(new ASM.Instruction.BinaryInstr("add", curBlock, PhysicalReg.get("t0"), PhysicalReg.get("t0"), PhysicalReg.get("t1")));
        int offsetResult = curBlock.parent.getVirtualReg(instr.result.name);
        addInstrWithOverflowedImm("sw", PhysicalReg.get("t0"), offsetResult, PhysicalReg.get("sp"));
    }
    public void visit(IcmpInstr instr) {
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        loadReg(instr.lhs, PhysicalReg.get("t0"));
        loadReg(instr.rhs, PhysicalReg.get("t1"));
        curBlock.addInstr(new ASM.Instruction.BinaryInstr("sub", curBlock, PhysicalReg.get("t2"), PhysicalReg.get("t0"), PhysicalReg.get("t1")));
        switch (instr.cond) {
            case "eq" -> curBlock.addInstr(new SetInstr("seqz", curBlock, PhysicalReg.get("t2"), PhysicalReg.get("t2")));
            case "ne" -> curBlock.addInstr(new SetInstr("snez", curBlock, PhysicalReg.get("t2"), PhysicalReg.get("t2")));
            case "slt" -> curBlock.addInstr(new SetInstr("sltz", curBlock, PhysicalReg.get("t2"), PhysicalReg.get("t2")));
            case "sgt" -> curBlock.addInstr(new SetInstr("sgtz", curBlock, PhysicalReg.get("t2"), PhysicalReg.get("t2")));
            case "sge" -> {
                curBlock.addInstr(new SetInstr("sltz", curBlock, PhysicalReg.get("t2"), PhysicalReg.get("t2")));
                curBlock.addInstr(new BinaryImmInstr("xori", curBlock, PhysicalReg.get("t2"), PhysicalReg.get("t2"), 1));
            }
            case "sle" -> {
                curBlock.addInstr(new SetInstr("sgtz", curBlock, PhysicalReg.get("t2"), PhysicalReg.get("t2")));
                curBlock.addInstr(new BinaryImmInstr("xori", curBlock, PhysicalReg.get("t2"), PhysicalReg.get("t2"), 1));
            }
        }
        int offset = curBlock.parent.getVirtualReg(instr.result.name);
        addInstrWithOverflowedImm("sw", PhysicalReg.get("t2"), offset, PhysicalReg.get("sp"));
    }
    public void visit(LoadInstr instr) {
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        int offsetResult = curBlock.parent.getVirtualReg(instr.result.name);
        if (loadReg(instr.pointer, PhysicalReg.get("t0")) == null) {
            int offset = curBlock.parent.getAllocPtr(instr.pointer.name);
            addInstrWithOverflowedImm("lw", PhysicalReg.get("t0"), offset, PhysicalReg.get("sp"));
        } else addInstrWithOverflowedImm("lw", PhysicalReg.get("t0"), 0, PhysicalReg.get("t0"));
        addInstrWithOverflowedImm("sw", PhysicalReg.get("t0"), offsetResult, PhysicalReg.get("sp"));
    }
    public void visit(PhiInstr instr) {}
    public void visit(IR.Instruction.RetInstr instr) {
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        if (instr.value != null) loadReg(instr.value, PhysicalReg.get("a0"));
        addInstrWithOverflowedImm("lw", PhysicalReg.get("ra"), curBlock.parent.getRetReg(), PhysicalReg.get("sp"));
        addInstrWithOverflowedImm("add", PhysicalReg.get("sp"), curBlock.parent.stackSize, PhysicalReg.get("sp"));
        curBlock.addInstr(new ASM.Instruction.RetInstr(curBlock));
    }
    public void visit(SelectInstr instr) {
        // unused
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        int offset = curBlock.parent.getVirtualReg(instr.result.name);
        loadReg(instr.cond, PhysicalReg.get("t0"));
        Block selectFalse = new Block(curBlock.parent, String.format("select_false.%d", program.selectCnt++));
        curBlock.addInstr(new BnezInstr(curBlock, PhysicalReg.get("t0"), selectFalse.label));
        loadReg(instr.lhs, PhysicalReg.get("t1"));
        addInstrWithOverflowedImm("sw", PhysicalReg.get("t1"), offset, PhysicalReg.get("sp"));
        curBlock.parent.addBlock(selectFalse);
        curBlock = selectFalse;
        loadReg(instr.rhs, PhysicalReg.get("t1"));
        addInstrWithOverflowedImm("sw", PhysicalReg.get("t1"), offset, PhysicalReg.get("sp"));
    }
    public void visit(StoreInstr instr) {
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        PhysicalReg src = loadReg(instr.value, PhysicalReg.get("t0"));
        if (loadReg(instr.pointer, PhysicalReg.get("t1")) == null) {
            int offset = curBlock.parent.getAllocPtr(instr.pointer.name);
            addInstrWithOverflowedImm("sw", src, offset, PhysicalReg.get("sp"));
        } else addInstrWithOverflowedImm("sw", src, 0, PhysicalReg.get("t1"));
    }

    public void visit(FuncDeclMod mod) {}
    public void visit(IR.Module.FuncDefMod mod) {
        var curSection = program.getSection(".text");
        var newFunc = new ASM.Module.FuncDefMod(curSection, mod);
        for (int i = 0; i < mod.params.size(); i++)
            newFunc.paramMap.put(mod.params.get(i).name, i);
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
                if (instr instanceof CallInstr)
                    newFunc.argCnt = Math.max(newFunc.argCnt, ((CallInstr) instr).args.size());
            }
        newFunc.stackSize = (newFunc.argCnt + newFunc.virtualRegCnt + newFunc.allocCnt + 1) * 4;
        if (newFunc.stackSize % 16 != 0) newFunc.stackSize = ((newFunc.stackSize) / 16 + 1) * 16;
        addInstrWithOverflowedImm("add", PhysicalReg.get("sp"), -newFunc.stackSize, PhysicalReg.get("sp"));
        addInstrWithOverflowedImm("sw", PhysicalReg.get("ra"), newFunc.getRetReg(), PhysicalReg.get("sp"));
        for (var block : mod.body)
            block.accept(this);
    }
    public void visit(IR.Module.GlobalVarDefMod mod) {
        var curSection = program.getSection(".data");
        curSection.addModule(new ASM.Module.GlobalVarDefMod(curSection, mod.globalVar.name, mod.init.isNull ? 0 : mod.init.value));
    }
    public void visit(IR.Module.StringLiteralDefMod mod) {
        var curSection = program.getSection(".rodata");
        curSection.addModule(new ASM.Module.StringLiteralMod(curSection, mod.ptr.name, mod.value, mod.value.length() + 1));
    }
    public void visit(StructDefMod mod) {}
}
