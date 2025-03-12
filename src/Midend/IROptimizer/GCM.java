package Midend.IROptimizer;

import IR.IRBlock;
import IR.IRProgram;
import IR.Instruction.Instruction;
import IR.Instruction.PhiInstr;
import IR.Module.FuncDefMod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class GCM {
    private final IRProgram program;

    private HashSet<Instruction> visited;
    private HashMap<Instruction, IRBlock> earlyBBMap;
    private HashMap<Instruction, IRBlock> bestBBMap;
    private IRBlock root;
    private FuncDefMod curFunc;

    public GCM(IRProgram program) {
        this.program = program;
    }

    public void run() {
        program.funcDefs.forEach(this::runFunc);
        if (program.initFunc != null) runFunc(program.initFunc);
        runFunc(program.mainFunc);
    }

    private void runFunc(FuncDefMod func) {
        // 遍历支配树，计算每个块的深度
        traverseDomTree(func.body.getFirst());
        // 遍历控制流图，计算每个块的循环深度
        buildLoopNestTree(func);
        // 建立 def-use 链
        buildDefUseChain(func);
        // init
        visited = new HashSet<>();
        earlyBBMap = new HashMap<>();
        bestBBMap = new HashMap<>();
        root = func.body.getFirst();
        curFunc = func;
        // early
        for (var block : func.body) {
            for (var instr : block.instructions)
                if (instr.isPinned()) {
                    visited.add(instr);
                    earlyBBMap.put(instr, block);
                }
            for (var phiInstr : block.phiInstrs.values()) {
                visited.add(phiInstr);
                earlyBBMap.put(phiInstr, block);
            }
        }
        for (var block : func.body) {
            block.instructions.forEach(this::scheduleEarly);
            block.phiInstrs.values().forEach(this::scheduleEarly);
        }
        // late
        visited = new HashSet<>();
        for (var block : func.body) {
            for (var instr : block.instructions)
                if (instr.isPinned()) {
                    visited.add(instr);
                    bestBBMap.put(instr, block);
                }
            for (var phiInstr : block.phiInstrs.values()) {
                visited.add(phiInstr);
                bestBBMap.put(phiInstr, block);
            }
        }
        for (var block : func.body) {
            block.instructions.forEach(this::scheduleLate);
            block.phiInstrs.values().forEach(this::scheduleLate);
        }
        // Motion
        visited = new HashSet<>();
        HashSet<Instruction> instrSet = new HashSet<>();
        for (var block : func.body) {
            HashSet<Instruction> removeList = new HashSet<>();
            for (var instr : block.instructions) {
                instrSet.add(instr);
                if (bestBBMap.get(instr) == block) visited.add(instr);
                else removeList.add(instr);
            }
            block.instructions.removeAll(removeList);
        }
        for (var instr : instrSet) scheduleMotion(instr);
    }

    private void scheduleEarly(Instruction instr) {
        if (visited.contains(instr)) return;
        visited.add(instr);
        IRBlock earlyBB = root;
        for (var op : instr.getUse()) {
            Instruction opDef = curFunc.defInstrMap.get(op);
            if (opDef == null) continue; // def in params
            scheduleEarly(opDef);
            IRBlock opBB = earlyBBMap.get(opDef);
            if (opBB.depth > earlyBB.depth) earlyBB = opBB;
        }
        earlyBBMap.put(instr, earlyBB);
    }

    private void scheduleLate(Instruction instr) {
        if (visited.contains(instr)) return;
        visited.add(instr);
        IRBlock lca = null;
        for (var user : curFunc.useInstrMap.get(instr.getDef())) {
            scheduleLate(user);
            IRBlock bb = bestBBMap.get(user);
            if (user instanceof PhiInstr phiInstr)
                for (var pair : phiInstr.pairs)
                    if (pair.a == instr.getDef()) {
                        bb = pair.b;
                        break;
                    }
            lca = findLca(lca, bb);
        }
        if (lca == null) lca = instr.parent;
        IRBlock bestBB = lca, curBB = lca, earlyBB = earlyBBMap.get(instr);
        while (curBB.depth >= earlyBB.depth) {
            if (curBB.loopDepth < bestBB.loopDepth) bestBB = curBB;
            if (curBB == curBB.father) break;
            else curBB = curBB.father;
        }
        bestBBMap.put(instr, bestBB);
    }

    private IRBlock findLca(IRBlock a, IRBlock b) {
        // TODO 线性算法，有待提高，不过无伤大雅
        if (a == null) return b;
        while (a.depth > b.depth) a = a.father;
        while (b.depth > a.depth) b = b.father;
        while (a != b) {
            a = a.father;
            b = b.father;
        }
        return a;
    }

    private void scheduleMotion(Instruction instr) {
        if (visited.contains(instr)) return;
        visited.add(instr);
        IRBlock bestBB = bestBBMap.get(instr);
        boolean isSameBB = false;
        for (var user : curFunc.useInstrMap.get(instr.getDef()))
            if (!(user instanceof PhiInstr)) {
                scheduleMotion(user);
                isSameBB |= bestBBMap.get(user) == bestBB;
            }
        if (!isSameBB) bestBB.instructions.add(bestBB.instructions.size() - 1, instr);
        else bestBB.instructions.addFirst(instr);
    }

    private void traverseDomTree(IRBlock cur) {
        if (cur.idom != null) cur.depth = cur.idom.depth + 1;
        for (var child : cur.children) traverseDomTree(child);
    }

    private void buildLoopNestTree(FuncDefMod func) {
        // 在一个循环中就深度++，多重循环嵌套时在里层的循环深度更深
        for (var block : func.body) {
            Stack<IRBlock> stack = new Stack<>();
            HashSet<IRBlock> loopBody = new HashSet<>();
            for (var pred : block.pred)
                if (pred.dom.get(block.blockNo)) {
                    stack.push(pred);
                    loopBody.add(pred);
                }
            while (!stack.isEmpty()) {
                IRBlock loopBodyBB = stack.pop();
                loopBodyBB.loopDepth++;
                for (var pred : loopBodyBB.pred)
                    if (!loopBody.contains(pred)) {
                        stack.push(pred);
                        loopBody.add(pred);
                    }
            }
            if (!loopBody.isEmpty()) block.loopDepth++;
        }
    }

    private void buildDefUseChain(FuncDefMod func) {
        func.useInstrMap.clear();
        func.defInstrMap.clear();
        for (var block : func.body) {
            for (var instr : block.instructions) {
                if (instr.getDef() != null) func.defInstrMap.put(instr.getDef(), instr);
                for (var use : instr.getUse()) {
                    if (func.useInstrMap.containsKey(use)) func.useInstrMap.get(use).add(instr);
                    else func.useInstrMap.put(use, new HashSet<>(Set.of(instr)));
                }
            }
            for (var phiInstr : block.phiInstrs.values()) {
                func.defInstrMap.put(phiInstr.getDef(), phiInstr);
                for (var use : phiInstr.getUse()) {
                    if (func.useInstrMap.containsKey(use)) func.useInstrMap.get(use).add(phiInstr);
                    else func.useInstrMap.put(use, new HashSet<>(Set.of(phiInstr)));
                }
            }
        }
    }
}
