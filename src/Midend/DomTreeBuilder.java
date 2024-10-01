package Midend;

import IR.IRBlock;
import IR.IRProgram;
import IR.Module.FuncDefMod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Queue;

public class DomTreeBuilder {
    private final IRProgram program;

    public DomTreeBuilder(IRProgram program) {
        this.program = program;
    }

    public void build() {
        program.funcDefs.forEach(this::buildFunc);
    }

    public void buildFunc(FuncDefMod func) {
        boolean[] visited = new boolean[func.body.size()];
        ArrayList<IRBlock> ord = new ArrayList<>();
        IRBlock entry = func.body.getFirst();
        // Inverse post order
        Traverse(ord, visited, entry);
        // Dominator Tree
        for (int i = ord.size() - 1; i >= 0; --i) {
            // Dominator Set
            var block = ord.get(i);
            BitSet dom = new BitSet(func.body.size());
            BitSet self = new BitSet(func.body.size());
            dom.set(0, func.body.size(), true);
            self.set(block.blockNo, true);
            for (var pred : block.pred) dom.and(pred.dom);
            dom.or(self);
            block.dom = dom;
            // Immediate Dominator
            for (int j = 0; j < func.body.size(); ++j)
                if (block.dom.get(j) && func.body.get(j).dom.cardinality() == block.dom.cardinality()) {
                    block.idom = func.body.get(j);
                    break;
                }
        }
        // 节点可以是自己的支配边界（循环支配的情况）
        // Dominance Frontier and Children
        for (var block : func.body) {
            for (var suc : block.suc)
                if (suc.idom == block) block.children.add(suc);
            // TODO : Dominance Frontier
        }
    }

    // Inverse Post Order
    private void Traverse(ArrayList<IRBlock> ord, boolean[] visited, IRBlock block) {
        visited[block.blockNo] = true;
        for (var suc : block.suc)
            if (!visited[block.blockNo]) Traverse(ord, visited, suc);
        ord.add(block);
    }
}
