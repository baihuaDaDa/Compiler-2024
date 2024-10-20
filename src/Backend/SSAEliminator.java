package Backend;

import ASM.Instruction.*;
import ASM.Module.Block;
import IR.IRBlock;
import IR.IRProgram;
import IR.Module.FuncDefMod;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRGlobalPtr;
import Util.IRObject.IREntity.IRLiteral;
import Util.IRObject.IREntity.IRLocalVar;
import Util.PhysicalReg;
import org.antlr.v4.runtime.misc.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SSAEliminator {
    private final FuncDefMod func;
    private Block curBlock;

    public SSAEliminator(FuncDefMod func) {
        this.func = func;
    }

    public void run() {
        func.body.forEach(this::runBlock);
    }

    private void addInstrBeforeJumpWithOverflowedImm(String instr, PhysicalReg reg1, int imm, PhysicalReg reg2) {
        if (imm < -2048 || imm > 2047) {
            // $t2 始终被占用了！！！
            // TODO 有没有可能不用$t2
            curBlock.addInstrBeforeJump(new LiInstr(curBlock, PhysicalReg.get("t2"), imm));
            switch (instr) {
                case "lw" -> {
                    curBlock.addInstrBeforeJump(new ASM.Instruction.BinaryInstr("add", curBlock, PhysicalReg.get("t2"), PhysicalReg.get("t2"), reg2));
                    curBlock.addInstrBeforeJump(new LwInstr(curBlock, reg1, 0, PhysicalReg.get("t2")));
                }
                case "sw" -> {
                    curBlock.addInstrBeforeJump(new ASM.Instruction.BinaryInstr("add", curBlock, PhysicalReg.get("t2"), PhysicalReg.get("t2"), reg2));
                    curBlock.addInstrBeforeJump(new SwInstr(curBlock, reg1, 0, PhysicalReg.get("t2")));
                }
                default ->
                        curBlock.addInstrBeforeJump(new ASM.Instruction.BinaryInstr(instr, curBlock, reg1, reg2, PhysicalReg.get("t2")));
            }
        } else switch (instr) {
            case "lw" -> curBlock.addInstrBeforeJump(new LwInstr(curBlock, reg1, imm, reg2));
            case "sw" -> curBlock.addInstrBeforeJump(new SwInstr(curBlock, reg1, imm, reg2));
            default -> curBlock.addInstrBeforeJump(new ASM.Instruction.BinaryImmInstr(instr + "i", curBlock, reg1, reg2, imm));
        }
    }

    private Pair<PhysicalReg, Integer> loadRegBeforeJump(IREntity entity, PhysicalReg dst, boolean isLeft) {
        switch (entity) {
            case IRLiteral literal -> {
                // TODO 立即数可以不用存进寄存器
                curBlock.addInstrBeforeJump(new LiInstr(curBlock, dst, literal.value));
                return new Pair<>(dst, 0);
            }
            case IRLocalVar localVar -> {
                // 如果是 a 系列寄存器说明此时希望直接 load 到 dst 上
                if (curBlock.parent.isPhysicalReg(localVar.name)){
                    if (dst == null || dst.name.charAt(0) != 'a') return new Pair<>(curBlock.parent.getReg(localVar.name), 0);
                    curBlock.addInstrBeforeJump(new MvInstr(curBlock, dst, curBlock.parent.getReg(localVar.name)));
                    return new Pair<>(dst, 0);
                }
                else if (curBlock.parent.isParamReg(localVar.name)) {
                    assert !isLeft;
                    int index = curBlock.parent.paramMap.get(localVar.name);
                    if (index < 8) {
                        if (dst.name.charAt(0) != 'a' || dst == PhysicalReg.get("a" + index)) return new Pair<>(PhysicalReg.get("a" + index), 0);
                        curBlock.addInstrBeforeJump(new MvInstr(curBlock, dst, PhysicalReg.get("a" + index)));
                        return new Pair<>(dst, 0);
                    } else {
                        int offset = curBlock.parent.getParamReg(localVar.name);
                        addInstrBeforeJumpWithOverflowedImm("lw", dst, offset, PhysicalReg.get("sp"));
                        return new Pair<>(dst, 0);
                    }
                } else if (curBlock.parent.isSpilledVar(localVar.name)) {
                    int offset = curBlock.parent.getSpilledVar(localVar.name);
                    if (isLeft) return new Pair<>(null, offset);
                    addInstrBeforeJumpWithOverflowedImm("lw", dst, offset, PhysicalReg.get("sp"));
                    return new Pair<>(dst, 0);
                } else throw new RuntimeException("Unknown local variable");
            }
            case IRGlobalPtr irGlobalPtr -> {
                curBlock.addInstrBeforeJump(new LaInstr(curBlock, dst, irGlobalPtr.name));
                // global var -> ptr to the value, string literal -> ptr of the head
                return new Pair<>(dst, 0);
            }
            case null, default -> throw new RuntimeException("Unknown entity");
        }
    }

    // 寻找基环。若存在基环，则返回环中任意一个节点；若不存在，则返回树的根节点
    private HashSet<IREntity> FindRoot(HashMap<IREntity, IREntity> predecessors) {
        HashSet<IREntity> roots = new HashSet<>();
        HashSet<IREntity> visited = new HashSet<>();
        for (var root : predecessors.keySet()) {
            if (visited.contains(root)) continue;
            while (!visited.contains(root)) {
                visited.add(root);
                if (!predecessors.containsKey(root)) break;
                root = predecessors.get(root);
            }
            roots.add(root);
        }
        return roots;
    }

    /** 后序遍历外向基环图
     * @param isTree 记录是否构成外向基环图，若为 true 则为树。
     */
    private void DfsErt(IREntity cur, IREntity root, boolean isTree, HashMap<IREntity, HashSet<IRLocalVar>> successors, HashMap<IREntity, IREntity> predecessors) {
        var sucs = successors.get(cur);
        PhysicalReg backup = null;
        if (cur == root && !isTree) backup = loadRegBeforeJump(cur, PhysicalReg.get("t1"), false).a; // 备份
        for (var suc : sucs) {
            if (suc != root) DfsErt(suc, root, isTree, successors, predecessors);
            var result = loadRegBeforeJump(suc, null, true);
            PhysicalReg src = (backup != null ? backup : loadRegBeforeJump(cur, PhysicalReg.get("t0"), false).a);
            if (result.a != null) curBlock.addInstrBeforeJump(new MvInstr(curBlock, result.a, src));
            else addInstrBeforeJumpWithOverflowedImm("sw", src, result.b, PhysicalReg.get("sp"));
        }
    }

    public void runBlock(IRBlock block) {
        HashMap<IREntity, HashSet<IRLocalVar>> successors = new HashMap<>();
        HashMap<IREntity, IREntity> predecessors = new HashMap<>();
        for (var phiInstr : block.phiInstrs.values())
            for (var branch : phiInstr.pairs) {
                if (branch.b != block) {
                    curBlock = branch.b.asmBlock;
                    var result = loadRegBeforeJump(phiInstr.result, null, true);
                    PhysicalReg src = loadRegBeforeJump(branch.a, PhysicalReg.get("t0"), false).a;
                    if (result.a != null) curBlock.addInstrBeforeJump(new MvInstr(curBlock, result.a, src));
                    else addInstrBeforeJumpWithOverflowedImm("sw", src, result.b, PhysicalReg.get("sp"));
                } else {
                    if (!successors.containsKey(branch.a)) successors.put(branch.a, new HashSet<>(List.of(phiInstr.result)));
                    else successors.get(branch.a).add(phiInstr.result);
                    predecessors.put(phiInstr.result, phiInstr.result);
                }
            }
        if (predecessors.isEmpty()) return;
        curBlock = block.asmBlock;
        // find ring or root
        var roots = FindRoot(predecessors);
        // dfs
        for (var root : roots) DfsErt(root, root, !predecessors.containsKey(root), successors, predecessors);
    }
}
