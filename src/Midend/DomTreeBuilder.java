package Midend;

import IR.IRBlock;
import IR.IRProgram;
import IR.Module.FuncDefMod;

import java.util.ArrayList;
import java.util.BitSet;

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
        boolean converge = false;
        while (!converge) {
            converge = true;
            for (int i = ord.size() - 1; i >= 0; --i) {
                // Dominator Set
                var block = ord.get(i);
                BitSet dom = new BitSet(func.body.size());
                BitSet self = new BitSet(func.body.size());
                dom.set(0, func.body.size(), true);
                self.set(block.blockNo, true);
                for (var pred : block.pred) dom.and(pred.dom);
                dom.or(self);
                if (!dom.equals(block.dom)) {
                    block.dom = dom;
                    converge = false;
                }
            }
        }
        // Immediate Dominator
        for (var block : func.body)
            for (int j = 0; j < func.body.size(); ++j)
                if (block.dom.get(j) && func.body.get(j).dom.cardinality() == block.dom.cardinality()) {
                    block.idom = func.body.get(j);
                    break;
                }
        // 节点可以是自己的支配边界（循环支配的情况）
        // Dominance Frontier and Children
        for (var block : func.body) {
            for (var suc : block.suc)
                if (suc.idom == block) block.children.add(suc);
            BitSet dominators = new BitSet(func.body.size());
            BitSet self = new BitSet(func.body.size());
            self.set(block.blockNo, true);
            for (var pred : block.pred)
                dominators.or(minusSet(pred.dom, minusSet(block.dom, self)));
            for (int j = 0; j < func.body.size(); ++j)
                if (dominators.get(j)) func.body.get(j).domFrontier.add(block);
        }
    }

    // Inverse Post Order
    private void Traverse(ArrayList<IRBlock> ord, boolean[] visited, IRBlock block) {
        visited[block.blockNo] = true;
        for (var suc : block.suc)
            if (!visited[block.blockNo]) Traverse(ord, visited, suc);
        ord.add(block);
    }

    private BitSet minusSet(BitSet a, BitSet b) {
        // 00=0, 10=1, 01=0, 11=0
        BitSet c = (BitSet) a.clone();
        c.xor(b);
        c.and(a);
        return c;
    }
}
