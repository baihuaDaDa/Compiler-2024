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
import org.antlr.v4.runtime.misc.Pair;

import java.util.Objects;

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
    private Pair<PhysicalReg, Integer> loadReg(IREntity entity, PhysicalReg dst, boolean isLeft) {
        switch (entity) {
            case IRLiteral literal -> {
                // TODO 立即数可以不用存进寄存器
                curBlock.addInstr(new LiInstr(curBlock, dst, literal.value));
                return new Pair<>(dst, 0);
            }
            case IRLocalVar localVar -> {
                if (curBlock.parent.isPhysicalReg(localVar.name))
                    return new Pair<>(curBlock.parent.getReg(localVar.name), 0);
                else if (curBlock.parent.isParamReg(localVar.name)) {
                    assert !isLeft;
                    int index = curBlock.parent.paramMap.get(localVar.name);
                    if (index < 8) return new Pair<>(PhysicalReg.get("a" + index), 0);
                    else {
                        int offset = curBlock.parent.getParamReg(localVar.name);
                        addInstrWithOverflowedImm("lw", dst, offset, PhysicalReg.get("sp"));
                        return new Pair<>(dst, 0);
                    }
                } else if (curBlock.parent.isSpilledVar(localVar.name)) {
                    int offset = curBlock.parent.getSpilledVar(localVar.name);
                    if (isLeft) return new Pair<>(null, offset);
                    addInstrWithOverflowedImm("lw", dst, offset, PhysicalReg.get("sp"));
                    return new Pair<>(dst, 0);
                } else throw new RuntimeException("Unknown local variable");
            }
            case IRGlobalPtr irGlobalPtr -> {
                curBlock.addInstr(new LaInstr(curBlock, dst, irGlobalPtr.name));
                // global var -> ptr to the value, string literal -> ptr of the head
                return new Pair<>(dst, 0);
            }
            case null, default -> throw new RuntimeException("Unknown entity");
        }
    }

    public void visit(AllocaInstr instr) {}
    public void visit(IR.Instruction.BinaryInstr instr) {
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        PhysicalReg lhs = loadReg(instr.lhs, PhysicalReg.get("t0"), false).a;
        PhysicalReg rhs = loadReg(instr.rhs, PhysicalReg.get("t1"), false).a;
        var result = loadReg(instr.result, null, true);
        if (result.a != null) curBlock.addInstr(new ASM.Instruction.BinaryInstr(ASM.Instruction.BinaryInstr.getInstr(instr.op), curBlock, result.a, lhs, rhs));
        else {
            curBlock.addInstr(new ASM.Instruction.BinaryInstr(ASM.Instruction.BinaryInstr.getInstr(instr.op), curBlock, PhysicalReg.get("t0"), lhs, rhs));
            addInstrWithOverflowedImm("sw", PhysicalReg.get("t0"), result.b, PhysicalReg.get("sp"));
        }
    }
    public void visit(BrInstr instr) {
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        if (instr.cond != null) {
            PhysicalReg cond = loadReg(instr.cond, PhysicalReg.get("t0"), false).a;
            Block brTure = new Block(curBlock.parent, String.format("br_true.%d", program.brCnt++));
            curBlock.addInstr(new BnezInstr(curBlock, cond, brTure.label));
            curBlock.addInstr(new JInstr(curBlock, instr.elseBlock.label));
            curBlock.parent.addBlock(brTure);
            curBlock = brTure;
            curBlock.addInstr(new JInstr(curBlock, instr.thenBlock.label));
        } else curBlock.addInstr(new JInstr(curBlock, instr.thenBlock.label));
    }
    public void visit(IR.Instruction.CallInstr instr) {
        // TODO
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
        PhysicalReg index = loadReg(instr.indices.size() == 1 ? instr.indices.getFirst() : instr.indices.get(1), PhysicalReg.get("t1"), false).a; // load index
        curBlock.addInstr(new BinaryImmInstr("slli", curBlock, PhysicalReg.get("t1"), index, 2)); // index * sizeof(...)
        PhysicalReg ptr = loadReg(instr.pointer, PhysicalReg.get("t0"), false).a;
        var result = loadReg(instr.result, null, true);
        if (result.a != null) curBlock.addInstr(new ASM.Instruction.BinaryInstr("add", curBlock, result.a, ptr, PhysicalReg.get("t1")));
        else {
            curBlock.addInstr(new ASM.Instruction.BinaryInstr("add", curBlock, PhysicalReg.get("t0"), ptr, PhysicalReg.get("t1")));
            addInstrWithOverflowedImm("sw", PhysicalReg.get("t0"), result.b, PhysicalReg.get("sp"));
        }
    }
    public void visit(IcmpInstr instr) {
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        PhysicalReg lhs = loadReg(instr.lhs, PhysicalReg.get("t0"), false).a;
        PhysicalReg rhs = loadReg(instr.rhs, PhysicalReg.get("t1"), false).a;
        curBlock.addInstr(new ASM.Instruction.BinaryInstr("sub", curBlock, PhysicalReg.get("t0"), lhs, rhs));
        PhysicalReg result;
        boolean isSpilled = false;
        if (curBlock.parent.isPhysicalReg(instr.result.name)) result = curBlock.parent.getReg(instr.result.name);
        else {
            result = PhysicalReg.get("t0");
            isSpilled = true;
        }
        switch (instr.cond) {
            case "eq" -> curBlock.addInstr(new SetInstr("seqz", curBlock, result, PhysicalReg.get("t0")));
            case "ne" -> curBlock.addInstr(new SetInstr("snez", curBlock, result, PhysicalReg.get("t0")));
            case "slt" -> curBlock.addInstr(new SetInstr("sltz", curBlock, result, PhysicalReg.get("t0")));
            case "sgt" -> curBlock.addInstr(new SetInstr("sgtz", curBlock, result, PhysicalReg.get("t0")));
            case "sge" -> {
                curBlock.addInstr(new SetInstr("sltz", curBlock, PhysicalReg.get("t0"), PhysicalReg.get("t0")));
                curBlock.addInstr(new BinaryImmInstr("xori", curBlock, result, PhysicalReg.get("t0"), 1));
            }
            case "sle" -> {
                curBlock.addInstr(new SetInstr("sgtz", curBlock, PhysicalReg.get("t0"), PhysicalReg.get("t0")));
                curBlock.addInstr(new BinaryImmInstr("xori", curBlock, result, PhysicalReg.get("t0"), 1));
            }
        }
        if (isSpilled) {
            int offset = curBlock.parent.getSpilledVar(instr.result.name);
            addInstrWithOverflowedImm("sw", result, offset, PhysicalReg.get("sp"));
        }
    }
    public void visit(LoadInstr instr) {
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        PhysicalReg src = loadReg(instr.pointer, PhysicalReg.get("t0"), false).a;
        var result = loadReg(instr.result, null, true);
        if (result.a != null) addInstrWithOverflowedImm("lw", result.a, 0, src);
        else {
            addInstrWithOverflowedImm("lw", PhysicalReg.get("t1"), 0, src);
            addInstrWithOverflowedImm("sw", PhysicalReg.get("t1"), result.b, PhysicalReg.get("sp"));
        }
    }
    public void visit(PhiInstr instr) {}
    public void visit(IR.Instruction.RetInstr instr) {
        // TODO
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        if (instr.value != null) loadReg(instr.value, PhysicalReg.get("a0"));
        addInstrWithOverflowedImm("lw", PhysicalReg.get("ra"), curBlock.parent.getRetReg(), PhysicalReg.get("sp"));
        addInstrWithOverflowedImm("add", PhysicalReg.get("sp"), curBlock.parent.stackSize, PhysicalReg.get("sp"));
        curBlock.addInstr(new ASM.Instruction.RetInstr(curBlock));
    }
    public void visit(SelectInstr instr) {
        // unused
        /*
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
        */
    }
    public void visit(StoreInstr instr) {
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        PhysicalReg src = loadReg(instr.value, PhysicalReg.get("t0"), false).a;
        PhysicalReg dst = loadReg(instr.pointer, PhysicalReg.get("t1"), false).a;
        addInstrWithOverflowedImm("sw", src, 0, dst);
    }

    public void visit(FuncDeclMod mod) {}
    public void visit(IR.Module.FuncDefMod mod) {
        var curSection = program.getSection(".text");
        var newFunc = new ASM.Module.FuncDefMod(curSection, mod);
        for (int i = 0; i < mod.params.size(); i++)
            newFunc.paramMap.put(mod.params.get(i).name, i);
        for (var entry : mod.regMap.entrySet())
            newFunc.regMap.put(entry.getKey().name, entry.getValue());
        curSection.addModule(newFunc);
        curBlock = newFunc.body.getFirst();
        for (var block : mod.body)
            for  (var instr : block.instructions) {
                if (instr instanceof CallInstr)
                    newFunc.argCnt = Math.max(newFunc.argCnt, ((CallInstr) instr).args.size());
            }
        for (var spilledVar : mod.spilledVars)
            newFunc.spilledVarMap.put(spilledVar.name, newFunc.spilledVarCnt++);
        newFunc.stackSize = (newFunc.argCnt + newFunc.spilledVarCnt + 1) * 4;
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
