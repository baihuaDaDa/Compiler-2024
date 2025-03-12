package Midend.IROptimizer.Util;

import IR.IRBlock;
import IR.IRProgram;
import IR.Module.FuncDefMod;

import java.util.*;

// 对 CFG 的反图建立支配树（控制依赖图）

public class CDGBuilder {
    private final IRProgram program;

    public CDGBuilder(IRProgram program) {
        this.program = program;
    }

    public void build() {
        // 交换前驱和后继，构建反图
        reverse();
        // 清空原有支配树信息
        clear();
        // 建立反图的支配树
        buildAntiDomTree();
        // 恢复 CFG
        reverse();
    }

    private void reverse() {
        program.funcDefs.forEach(this::reverseFunc);
        if (program.initFunc != null) reverseFunc(program.initFunc);
        reverseFunc(program.mainFunc);
    }

    private void reverseFunc(FuncDefMod func) {
        for (var block : func.body) {
            HashSet<IRBlock> tmp = block.suc;
            block.suc = block.pred;
            block.pred = tmp;
        }
    }

    private void clear() {
        program.funcDefs.forEach(this::clearFunc);
        if (program.initFunc != null) clearFunc(program.initFunc);
        clearFunc(program.mainFunc);
    }

    private void clearFunc(FuncDefMod func) {
        for (var block : func.body) {
            block.cdgDom = null;
            block.cdgIdom = null;
            block.cdgDomFrontier = new HashSet<>();
            block.cdgChildren = new HashSet<>();
        }
    }

    private void buildAntiDomTree() {
        program.funcDefs.forEach(this::buildAntiDomTreeFunc);
        if (program.initFunc != null) buildAntiDomTreeFunc(program.initFunc);
        buildAntiDomTreeFunc(program.mainFunc);
    }

    private void buildAntiDomTreeFunc(FuncDefMod func) {
        boolean[] visited = new boolean[func.body.size()];
        ArrayList<IRBlock> ord = new ArrayList<>();
        // BFS
        Traverse(func, ord, visited);
        // Dominator Tree
        boolean converge = false;
        while (!converge) {
            converge = true;
            for (int i = 0; i < ord.size(); ++i) {
                // Dominator Set
                var block = ord.get(i);
                BitSet dom = new BitSet(func.body.size());
                BitSet self = new BitSet(func.body.size());
                if (!block.pred.isEmpty()) dom.set(0, func.body.size(), true);
                self.set(block.blockNo, true);
                for (var pred : block.pred)
                    if (pred.cdgDom != null) dom.and(pred.cdgDom);
                dom.or(self);
                if (!dom.equals(block.cdgDom)) {
                    block.cdgDom = dom;
                    converge = false;
                }
            }
        }
        // Immediate Dominator
        for (var block : func.body) {
            if (block.cdgDom == null) continue;
            for (int j = 0; j < func.body.size(); ++j) {
                if (func.body.get(j).cdgDom == null) continue;
                if (block.cdgDom.get(j) && func.body.get(j).cdgDom.cardinality() == block.cdgDom.cardinality() - 1) {
                    block.cdgIdom = func.body.get(j);
                    break;
                }
            }
        }
        for (var block : func.body) block.cdgFather = block.cdgIdom == null ? block : block.cdgIdom;
        // 节点可以是自己的支配边界（循环支配的情况）
        // Dominance Frontier and Children
        for (var block : func.body) {
            if (block.cdgDom == null) continue;
            if (block.cdgIdom != null) block.cdgIdom.cdgChildren.add(block);
            BitSet dominators = new BitSet(func.body.size());
            BitSet self = new BitSet(func.body.size());
            self.set(block.blockNo, true);
            for (var pred : block.pred)
                dominators.or(minusSet(pred.cdgDom, minusSet(block.cdgDom, self)));
            for (int j = 0; j < func.body.size(); ++j) {
                if (func.body.get(j).cdgDom == null) continue;
                if (dominators.get(j)) func.body.get(j).cdgDomFrontier.add(block);
            }
        }
    }

    // BFS
    private void Traverse(FuncDefMod func, ArrayList<IRBlock> ord, boolean[] visited) {
        LinkedList<IRBlock> queue = new LinkedList<>();
        for (var block : func.body)
            if (block.pred.isEmpty()) {
                queue.add(block);
                visited[block.blockNo] = true;
            }
        while (!queue.isEmpty()) {
            var block = queue.pop();
            ord.add(block);
            for (var suc : block.suc)
                if (!visited[suc.blockNo]) {
                    queue.add(suc);
                    visited[suc.blockNo] = true;
                }
        }
    }

    private BitSet minusSet(BitSet a, BitSet b) {
        // 00=0, 10=1, 01=0, 11=0
        BitSet c = (BitSet) a.clone();
        c.xor(b);
        c.and(a);
        return c;
    }
}
