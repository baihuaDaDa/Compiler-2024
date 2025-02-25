package Backend;

import ASM.ASMProgram;
import ASM.Instruction.*;
import ASM.Module.Block;
import Backend.ASMOptimizer.SSAEliminator;
import Util.PhysicalReg;
import IR.IRBlock;
import IR.IRProgram;
import IR.IRVisitor;
import IR.Instruction.*;
import IR.Instruction.CallInstr;
import IR.Module.*;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRGlobalPtr;
import Util.IRObject.IREntity.IRLiteral;
import Util.IRObject.IREntity.IRLocalVar;
import org.antlr.v4.runtime.misc.Pair;

import java.util.*;

public class ASMBuilder implements IRVisitor {
    // TODO 注意 IR 的基本块不是按逻辑顺序的
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
        for (var instr : block.instructions) instr.accept(this);
    }

    // for lw: reg1 -> dst, reg2 -> base, imm -> offset
    // for sw: reg1 -> src, reg2 -> base, imm -> offset
    // for *i: reg1 -> dst, reg2 -> src, imm -> imm
    private void addInstrWithOverflowedImm(String instr, PhysicalReg reg1, int imm, PhysicalReg reg2) {
        if (imm < -2048 || imm > 2047) {
            // $t2 始终被占用了！！！
            // TODO 有没有可能不用$t2
            curBlock.addInstr(new LiInstr(curBlock, PhysicalReg.get("t2"), imm));
            switch (instr) {
                case "lw" -> {
                    curBlock.addInstr(new ASM.Instruction.BinaryInstr("add", curBlock, PhysicalReg.get("t2"), PhysicalReg.get("t2"), reg2));
                    curBlock.addInstr(new LwInstr(curBlock, reg1, 0, PhysicalReg.get("t2")));
                }
                case "sw" -> {
                    curBlock.addInstr(new ASM.Instruction.BinaryInstr("add", curBlock, PhysicalReg.get("t2"), PhysicalReg.get("t2"), reg2));
                    curBlock.addInstr(new SwInstr(curBlock, reg1, 0, PhysicalReg.get("t2")));
                }
                default -> curBlock.addInstr(new ASM.Instruction.BinaryInstr(instr, curBlock, reg1, reg2, PhysicalReg.get("t2")));
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
                return new Pair<>(dst, -1);
            }
            case IRLocalVar localVar -> {
                // 如果是 a 系列寄存器说明此时希望直接 load 到 dst 上
                if (curBlock.parent.isPhysicalReg(localVar.name)){
                    if (dst == null || dst.name.charAt(0) != 'a') return new Pair<>(curBlock.parent.getReg(localVar.name), 0);
                    curBlock.addInstr(new MvInstr(curBlock, dst, curBlock.parent.getReg(localVar.name)));
                    return new Pair<>(dst, -1);
                } else if (curBlock.parent.isParamReg(localVar.name)) {
                    assert !isLeft;
                    int offset = curBlock.parent.getParamReg(localVar.name);
                    addInstrWithOverflowedImm("lw", dst, offset, PhysicalReg.get("sp"));
                    return new Pair<>(dst, -1);
                } else if (curBlock.parent.isSpilledVar(localVar.name)) {
                    int offset = curBlock.parent.getSpilledVar(localVar.name);
                    if (isLeft) return new Pair<>(null, offset);
                    addInstrWithOverflowedImm("lw", dst, offset, PhysicalReg.get("sp"));
                    return new Pair<>(dst, -1);
                } else return new Pair<>(null, -1); // 不活跃变量
            }
            case IRGlobalPtr irGlobalPtr -> {
                curBlock.addInstr(new LaInstr(curBlock, dst, irGlobalPtr.name));
                // global var -> ptr to the value, string literal -> ptr of the head
                return new Pair<>(dst, -1);
            }
            case null, default -> throw new RuntimeException("Unknown entity");
        }
    }

    private void DfsErt(int cur, int root, boolean isTree, HashMap<Integer, HashSet<Integer>> successors, boolean[] visited) {
        visited[cur] = true;
        var sucs = successors.get(cur);
        if (sucs == null) return;
        PhysicalReg backup = null;
        if (cur == root && !isTree) {
            curBlock.addInstr(new MvInstr(curBlock, PhysicalReg.get("t1"), PhysicalReg.get("a" + cur)));
            backup = PhysicalReg.get("t1"); // 备份
        }
        for (var suc : sucs) {
            if (suc != root) DfsErt(suc, root, isTree, successors, visited);
            var result = PhysicalReg.get("a" + suc);
            PhysicalReg src = (backup != null ? backup : PhysicalReg.get("a" + cur));
            curBlock.addInstrBeforeJump(new MvInstr(curBlock, result, src));
        }
    }

    private void LoadArgs(CallInstr instr) {
        HashMap<Integer, Integer> predecessors = new HashMap<>();
        HashMap<Integer, HashSet<Integer>> successors = new HashMap<>();
        int maxArgCnt = Math.min(instr.args.size(), 8);
        // 需要并行，新参数可能覆盖旧参数
        for (int i = 0; i < instr.args.size(); i++) {
            var arg = instr.args.get(i);
            if (i < 8) {
                // 对 a0-a7 建图
                if (arg instanceof IRLocalVar && instr.parent.parent.params.contains(arg)) {
                    int paramIndex = instr.parent.parent.params.indexOf(arg);
                    if (paramIndex < maxArgCnt) {
                        predecessors.put(i, paramIndex);
                        if (successors.containsKey(paramIndex)) successors.get(paramIndex).add(i);
                        else successors.put(paramIndex, new HashSet<>(List.of(i)));
                    }
                }
            } else {
                // 先把溢出的参数存起来
                PhysicalReg argReg = loadReg(arg, PhysicalReg.get("t0"), false).a;
                int offset = curBlock.parent.getSpilledArg(i - 8);
                addInstrWithOverflowedImm("sw", argReg, offset, PhysicalReg.get("sp"));
            }
        }
        // 加载前八个新参数
        boolean[] visited = new boolean[8];
        for (int i : predecessors.keySet()) {
            if (visited[i]) continue;
            boolean isTree = false;
            // 寻找根节点
            int root = i;
            while (!visited[root]) {
                visited[root] = true;
                if (!predecessors.containsKey(root)) {
                    isTree = true;
                    break;
                }
                root = predecessors.get(root);
            }
            DfsErt(root, root, isTree, successors, visited);
        }
        // 最后给没有数据冲突的参数赋值
        for (int i = 0; i < maxArgCnt; i++) {
            if (predecessors.containsKey(i)) continue;
            var arg = instr.args.get(i);
            var src = loadReg(arg, PhysicalReg.get("t0"), false);
            curBlock.addInstr(new MvInstr(curBlock, PhysicalReg.get("a" + i), src.a));
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
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        // 保存 Caller Save 的寄存器
        var saveOrd = new ArrayList<>(instr.parent.parent.outMap.get(instr));
        var def = instr.getDef();
        int saveHead = curBlock.parent.getCallerSaveHead();
        for (var localVar : saveOrd) {
            if (localVar == def || !curBlock.parent.isPhysicalReg(localVar.name)) continue;
            PhysicalReg reg = curBlock.parent.regMap.get(localVar.name);
            if (PhysicalReg.isCalleeSaved(reg.name)) continue;
            addInstrWithOverflowedImm("sw", reg, saveHead, PhysicalReg.get("sp"));
            saveHead += 4;
        }
        // 录入被调用函数的参数
        LoadArgs(instr);
        curBlock.addInstr(new ASM.Instruction.CallInstr(curBlock, instr.funcName));
        if (instr.result != null) {
            var result = loadReg(instr.result, null, true);
            if (result.a != null || result.b != -1) {
                if (result.a != null) curBlock.addInstr(new MvInstr(curBlock, result.a, PhysicalReg.get("a0")));
                else addInstrWithOverflowedImm("sw", PhysicalReg.get("a0"), result.b, PhysicalReg.get("sp"));
            }
        }
        saveHead = curBlock.parent.getCallerSaveHead();
        for (var localVar : saveOrd) {
            if (localVar == def || !curBlock.parent.isPhysicalReg(localVar.name)) continue;
            PhysicalReg reg = curBlock.parent.regMap.get(localVar.name);
            if (PhysicalReg.isCalleeSaved(reg.name)) continue;
            addInstrWithOverflowedImm("lw", reg, saveHead, PhysicalReg.get("sp"));
            saveHead += 4;
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
        curBlock.addInstr(new CommentInstr(curBlock, instr.toString()));
        // save return value
        if (instr.value != null) loadReg(instr.value, PhysicalReg.get("a0"), false);
        // callee save reload
        if (!instr.parent.parent.funcName.equals("main")) {
            int saveHead = curBlock.parent.getCalleeSaveHead();
            for (int i = 0; i < curBlock.parent.calleeSaveCnt; i++) {
                addInstrWithOverflowedImm("lw", PhysicalReg.get("s" + i), saveHead, PhysicalReg.get("sp"));
                saveHead += 4;
            }
        }
        // load ra and restore sp
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
        for (int i = 8; i < mod.params.size(); i++)
            newFunc.paramMap.put(mod.params.get(i).name, i - 8);
        for (var entry : mod.regMap.entrySet())
            newFunc.regMap.put(entry.getKey().name, entry.getValue());
        curSection.addModule(newFunc);
        for (var block : mod.body)
            for  (var instr : block.instructions)
                if (instr instanceof CallInstr callInstr) {
                    newFunc.spilledArgCnt = Math.max(newFunc.spilledArgCnt, (callInstr.args.size() - 8));
                    var outs = callInstr.parent.parent.outMap.get(instr);
                    var def = callInstr.parent.parent.defMap.get(instr);
                    int nonSpilledCnt = 0;
                    for (var out : outs)
                        if (callInstr.parent.parent.regMap.containsKey(out)
                                && !PhysicalReg.isCalleeSaved(callInstr.parent.parent.regMap.get(out).name)
                                && out != def) nonSpilledCnt++;
                    newFunc.callerSaveCnt = Math.max(newFunc.callerSaveCnt, nonSpilledCnt);
                }
        for (var spilledVar : mod.spilledVars)
            newFunc.spilledVarMap.put(spilledVar.name, newFunc.spilledVarCnt++);
        newFunc.calleeSaveCnt = Math.min(Math.max(mod.activeCnt - 6, 0), 12);
        newFunc.stackSize = (newFunc.spilledArgCnt + newFunc.spilledVarCnt + newFunc.callerSaveCnt + newFunc.calleeSaveCnt + 1) * 4;
        if (newFunc.stackSize % 16 != 0) newFunc.stackSize = ((newFunc.stackSize) / 16 + 1) * 16;
        curBlock = newFunc.body.getFirst();
        addInstrWithOverflowedImm("add", PhysicalReg.get("sp"), -newFunc.stackSize, PhysicalReg.get("sp"));
        addInstrWithOverflowedImm("sw", PhysicalReg.get("ra"), newFunc.getRetReg(), PhysicalReg.get("sp"));
        if (!mod.funcName.equals("main")) {
            int saveHead = newFunc.getCalleeSaveHead();
            for (int i = 0; i < newFunc.calleeSaveCnt; i++) {
                addInstrWithOverflowedImm("sw", PhysicalReg.get("s" + i), saveHead, PhysicalReg.get("sp"));
                saveHead += 4;
            }
        }
        for (var block : mod.body) block.accept(this);
        // SSA 消除
        SSAEliminator ssaEliminator = new SSAEliminator(mod, newFunc);
        ssaEliminator.run();
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
