package Midend.IROptimizer.Util;

import IR.IRBlock;
import IR.IRProgram;
import IR.Module.FuncDefMod;

import java.util.HashSet;

// 对 CFG 的反图建立支配树（控制依赖图）
// 直接调用支配树，把块的前驱和后继交换即可

public class CDGBuilder {
    private final IRProgram program;

    public CDGBuilder(IRProgram program) {
        this.program = program;
    }

    public void build() {
        // 交换前驱和后继，构建反图
        reverse();
        // 建立反图的支配树
        DomTreeBuilder domTreeBuilder = new DomTreeBuilder(program);
        domTreeBuilder.build();
        // 建立控制依赖图
        loadCDG();
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

    private void loadCDG() {
        program.funcDefs.forEach(this::loadCDGFunc);
        if (program.initFunc != null) loadCDGFunc(program.initFunc);
        loadCDGFunc(program.mainFunc);
    }

    private void loadCDGFunc(FuncDefMod func) {
        for (var block : func.body) {
            block.cdgDomFrontier = new HashSet<>(block.domFrontier);
            block.cdgIdom = block.idom;
        }
    }
}
