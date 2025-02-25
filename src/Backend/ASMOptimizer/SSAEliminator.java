package Backend.ASMOptimizer;

import ASM.Instruction.*;
import ASM.Module.Block;
import IR.IRBlock;
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
    private final ASM.Module.FuncDefMod asmFunc;
    private Block curBlock;
    private HashMap<PhysicalReg, Color> regColorMap;
    private HashMap<Integer, Color> offsetColorMap;
    private HashMap<IREntity, Color> colorMap;

    public SSAEliminator(FuncDefMod func, ASM.Module.FuncDefMod asmFunc) {
        this.func = func;
        this.asmFunc = asmFunc;
        regColorMap = new HashMap<>();
        offsetColorMap = new HashMap<>();
        colorMap = new HashMap<>();
    }

    private class Color {
        PhysicalReg reg = null;
        int offset = -1;
        IRLiteral literal = null;
        IRGlobalPtr globalPtr = null;

        public Color(IREntity entity) {
            switch (entity) {
                case IRLocalVar localVar -> {
                    if (asmFunc.isPhysicalReg(localVar.name)) reg = asmFunc.getReg(localVar.name);
                    else if (asmFunc.isSpilledVar(localVar.name)) offset = asmFunc.getSpilledVar(localVar.name);
                    else if (asmFunc.isParamReg(localVar.name)) offset = asmFunc.getParamReg(localVar.name);
                    else throw new RuntimeException("Unknown local variable when constructing `Color`");
                }
                case IRLiteral literal -> this.literal = literal;
                case IRGlobalPtr globalPtr -> this.globalPtr = globalPtr;
                default -> throw new RuntimeException("Unknown entity when constructing `Color`");
            }
        }

//        @Override
//        public String toString() {
//            return reg != null ? reg.name : offset != -1 ? Integer.toString(offset) : literal != null ? literal.toString() : globalPtr.name;
//        }
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

    private Pair<PhysicalReg, Integer> loadRegBeforeJump(Color color, PhysicalReg dst, boolean isLeft, boolean isBackup) {
        if (color.reg != null) {
            if (!isBackup && (dst == null || dst.name.charAt(0) != 'a' || dst == color.reg)) return new Pair<>(color.reg, 0);
            curBlock.addInstrBeforeJump(new MvInstr(curBlock, dst, color.reg));
            return new Pair<>(dst, 0);
        } else if (color.offset != -1) {
            if (isLeft) return new Pair<>(null, color.offset);
            addInstrBeforeJumpWithOverflowedImm("lw", dst, color.offset, PhysicalReg.get("sp"));
            return new Pair<>(dst, 0);
        } else if (color.literal != null) {
            // TODO 立即数可以不用存进寄存器
            curBlock.addInstrBeforeJump(new LiInstr(curBlock, dst, color.literal.value));
            return new Pair<>(dst, 0);
        } else if (color.globalPtr != null) {
            curBlock.addInstrBeforeJump(new LaInstr(curBlock, dst, color.globalPtr.name));
            // global var -> ptr to the value, string literal -> ptr of the head
            return new Pair<>(dst, 0);
        } else throw new RuntimeException("Unknown entity");
    }

    /** 后序遍历外向基环图
     * @param isTree 记录是否构成外向基环图，若为 true 则为树。
     */
    private void DfsErt(Color cur, Color root, boolean isTree, HashMap<Color, HashSet<Color>> successors, HashMap<Color, Color> predecessors, HashSet<Color> visited) {
        visited.add(cur);
        var sucs = successors.get(cur);
        if (sucs == null) return;
        PhysicalReg backup = null;
        if (cur == root && !isTree && predecessors.get(cur) != cur) backup = loadRegBeforeJump(cur, PhysicalReg.get("t1"), false, true).a; // 备份
        for (var suc : sucs) {
            if (suc != root) DfsErt(suc, root, isTree, successors, predecessors, visited);
            var result = loadRegBeforeJump(suc, null, true, false);
            PhysicalReg src = (backup != null ? backup : loadRegBeforeJump(cur, PhysicalReg.get("t0"), false, false).a);
            if (result.a != null) curBlock.addInstrBeforeJump(new MvInstr(curBlock, result.a, src));
            else addInstrBeforeJumpWithOverflowedImm("sw", src, result.b, PhysicalReg.get("sp"));
        }
    }

    public void runBlock(IRBlock block) {
        HashMap<IRBlock, HashMap<Color, HashSet<Color>>> successors = new HashMap<>();
        HashMap<IRBlock, HashMap<Color, Color>> predecessors = new HashMap<>();
        HashSet<Color> colorSet = new HashSet<>();
        for (var phiInstr : block.phiInstrs.values()) {
            Color newColor;
            if (colorMap.containsKey(phiInstr.result)) newColor = colorMap.get(phiInstr.result);
            else {
                newColor = new Color(phiInstr.result);
                if (newColor.reg != null) {
                    if (!regColorMap.containsKey(newColor.reg)) regColorMap.put(newColor.reg, newColor);
                    else newColor = regColorMap.get(newColor.reg);
                } else if (newColor.offset != -1) {
                    if (!offsetColorMap.containsKey(newColor.offset)) offsetColorMap.put(newColor.offset, newColor);
                    else newColor = offsetColorMap.get(newColor.offset);
                }
                colorMap.put(phiInstr.result, newColor);
            }
            colorSet.add(newColor);
        }
        for (var phiInstr : block.phiInstrs.values())
            for (var branch : phiInstr.pairs) {
                if (!(branch.a instanceof IRLocalVar)) continue;
                Color predColor;
                if (colorMap.containsKey(branch.a)) predColor = colorMap.get(branch.a);
                else {
                    predColor = new Color(branch.a);
                    if (predColor.reg != null) {
                        if (!regColorMap.containsKey(predColor.reg)) regColorMap.put(predColor.reg, predColor);
                        else predColor = regColorMap.get(predColor.reg);
                    } else if (predColor.offset != -1) {
                        if (!offsetColorMap.containsKey(predColor.offset)) offsetColorMap.put(predColor.offset, predColor);
                        else predColor = offsetColorMap.get(predColor.offset);
                    }
                    colorMap.put(branch.a, predColor);
                }
                if (!colorSet.contains(predColor)) continue;
                if (!successors.containsKey(branch.b)) {
                    successors.put(branch.b, new HashMap<>());
                    predecessors.put(branch.b, new HashMap<>());
                }
                var blockSucs = successors.get(branch.b);
                var blockPreds = predecessors.get(branch.b);
                var sucColor = colorMap.get(phiInstr.result);
                if (!blockSucs.containsKey(predColor)) blockSucs.put(predColor, new HashSet<>(List.of(sucColor)));
                else blockSucs.get(predColor).add(sucColor);
                blockPreds.put(sucColor, predColor);
            }
        for (var preBlock : predecessors.keySet()) {
            curBlock = preBlock.asmBlock;
            var blockSucs = successors.get(preBlock);
            var blockPreds = predecessors.get(preBlock);
            // find ring or root
            HashSet<Color> visited = new HashSet<>();
            for (var root : blockPreds.keySet()) {
                if (visited.contains(root)) continue;
                // 寻找基环。若存在基环，则返回环中任意一个节点；若不存在，则返回树的根节点
                boolean isTree = false;
                while (!visited.contains(root)) {
                    visited.add(root);
                    if (!blockPreds.containsKey(root)) {
                        isTree = true;
                        break;
                    }
                    root = blockPreds.get(root);
                }
                DfsErt(root, root, isTree, blockSucs, blockPreds, visited); // 深搜外向基环图
            }
        }
        for (var phiInstr : block.phiInstrs.values())
            for (var branch : phiInstr.pairs) {
                var predColor = colorMap.get(phiInstr.result);
                if (predecessors.containsKey(branch.b) && predecessors.get(branch.b).containsKey(predColor)) continue;
                curBlock = branch.b.asmBlock;
                var result = loadRegBeforeJump(predColor, null, true, false);
                PhysicalReg src = loadRegBeforeJump(new Color(branch.a), PhysicalReg.get("t0"), false, false).a;
                if (result.a != null) curBlock.addInstrBeforeJump(new MvInstr(curBlock, result.a, src));
                else addInstrBeforeJumpWithOverflowedImm("sw", src, result.b, PhysicalReg.get("sp"));
            }
    }
}
