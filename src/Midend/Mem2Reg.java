package Midend;

import IR.Instruction.*;
import IR.Module.FuncDefMod;
import IR.IRBlock;
import IR.IRProgram;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRGlobalPtr;
import Util.IRObject.IREntity.IRLiteral;
import Util.IRObject.IREntity.IRLocalVar;
import Util.Type.IRType;

import java.lang.reflect.Array;
import java.util.*;

public class Mem2Reg {
    private final IRProgram program;
    private final HashMap<String, IRType> allocVars;
    private final HashMap<String, HashSet<IRBlock>> defs;
    private final HashMap<String, Integer> nums;
    private final HashMap<String, Stack<IREntity>> valStacks;
    private final HashMap<IRLocalVar, IREntity> renameMap;
    private int criticalEdgeCnt = 0;

    public Mem2Reg(IRProgram program) {
        this.program = program;
        this.allocVars = new HashMap<>();
        this.defs = new HashMap<>();
        this.nums = new HashMap<>();
        this.valStacks = new HashMap<>();
        this.renameMap = new HashMap<>();
    }

    public void run() {
        DomTreeBuilder domTreeBuilder = new DomTreeBuilder(program);
        domTreeBuilder.build();
        program.funcDefs.forEach(this::runFunc);
        if (program.initFunc != null) runFunc(program.initFunc);
        runFunc(program.mainFunc);
    }

    private void runFunc(FuncDefMod func) {
        collectAllocVarsAndDefs(func);
        placePhi(func);
        rename(func.body.getFirst());
        boolean[] visited = new boolean[func.body.size()];
        splitCriticalEdge(func);
        reinit();
    }

    private void reinit() {
        allocVars.clear();
        defs.clear();
        nums.clear();
        valStacks.clear();
        renameMap.clear();
    }

    private void splitCriticalEdge(FuncDefMod func) {
        ArrayList<IRBlock> edgeList = new ArrayList<>();
        for (var block : func.body) {
            var sucCopy = new HashSet<>(block.suc);
            for (var suc : sucCopy) {
                if (block.suc.size() > 1 && suc.pred.size() > 1) {
                    IRBlock newBlock = new IRBlock(block.parent, String.format("critical_edge.%d", criticalEdgeCnt++));
                    newBlock.addInstr(new BrInstr(newBlock, null, suc, null));
                    // 不需要维护 blockNo，因为在之后的死代码块消除中 blockNo 总会失效
                    edgeList.add(newBlock);
                    var br = (BrInstr) block.instructions.getLast();
                    if (br.thenBlock == suc) br.thenBlock = newBlock;
                    else br.elseBlock = newBlock;
                    for (var phiInstr : suc.phiInstrs.values()) phiInstr.changeBlock(newBlock, block);
                    block.suc.remove(suc);
                    block.suc.add(newBlock);
                    newBlock.pred.add(block);
                    suc.pred.remove(block);
                    suc.pred.add(newBlock);
                    newBlock.suc.add(suc);
                }
            }
        }
        func.body.addAll(edgeList);
    }

    private void rename(IRBlock block) {
        block.phiInstrs.forEach((name, instr) -> {
            if (allocVars.containsKey(name)) {
                int no = nums.get(name);
                instr.result = new IRLocalVar("phi." + name + "." + (++no), instr.result.type);
                nums.replace(name, no);
                valStacks.get(name).push(instr.result);
            }
        });
        ArrayList<Instruction> instrs = block.instructions;
        ArrayList<Instruction> removeList = new ArrayList<>();
        for (var instr : instrs) {
            switch (instr) {
                case AllocaInstr allocaInstr -> removeList.add(instr);
                case StoreInstr storeInstr -> {
                    storeInstr.rename(renameMap);
                    if (storeInstr.pointer instanceof IRLocalVar && valStacks.containsKey(storeInstr.pointer.name)) {
                        var stack = valStacks.get(storeInstr.pointer.name);
                        if (!(storeInstr.value instanceof IRLiteral) || !storeInstr.value.type.isSameType(new IRType("ptr")))
                            stack.push(storeInstr.value);
                        removeList.add(instr);
                    }
                }
                case LoadInstr loadInstr -> {
                    loadInstr.rename(renameMap);
                    if (loadInstr.pointer instanceof IRLocalVar && valStacks.containsKey(loadInstr.pointer.name)) {
                        var stack = valStacks.get(loadInstr.pointer.name);
                        removeList.add(instr);
                        if (stack.empty()) continue;
                        renameMap.put(loadInstr.result, stack.peek());
                    }
                }
                default -> instr.rename(renameMap);
            }
        }
        instrs.removeAll(removeList);
        for (var var : allocVars.keySet())
            for (var suc : block.suc)
                if (suc.phiInstrs.containsKey(var)) {
                    if (valStacks.get(var).empty()) {
                        IREntity val;
                        IRType valType = suc.phiInstrs.get(var).result.type;
                        if (valType.isSameType(new IRType("ptr"))) val = new IRLiteral(valType, true);
                        else val = new IRLiteral(valType, 0);
                        suc.phiInstrs.get(var).addBranch(val, block);
                    } else suc.phiInstrs.get(var).addBranch(valStacks.get(var).peek(), block);
                }
        HashMap<String, Integer> valStackSizes = new HashMap<>();
        for (var dom : block.children) {
            valStacks.forEach((val, stack) -> valStackSizes.put(val, stack.size()));
            rename(dom);
            valStacks.forEach((val, stack) -> {
                while (valStackSizes.get(val) < stack.size()) stack.pop();
            });
        }
    }

    private void placePhi(FuncDefMod func) {
        for (var var : allocVars.keySet()) {
            Queue<IRBlock> queue = new ArrayDeque<>(defs.get(var));
            while (!queue.isEmpty()) {
                IRBlock block = queue.poll();
                for (var domFrontier : block.domFrontier)
                    if (!domFrontier.phiInstrs.containsKey(var)) {
                        PhiInstr phiInstr = new PhiInstr(domFrontier, new IRLocalVar(var, allocVars.get(var)));
                        domFrontier.addPhiInstr(phiInstr);
                        if (!domFrontier.domFrontier.isEmpty()) queue.add(domFrontier);
                    }
            }
        }
    }

    private void collectAllocVarsAndDefs(FuncDefMod func) {
        for (var block : func.body) {
            for (var instr : block.instructions) {
                if (instr instanceof AllocaInstr allocaInstr) {
                    allocVars.put(allocaInstr.result.name, allocaInstr.type);
                    defs.put(allocaInstr.result.name, new HashSet<>());
                    nums.put(allocaInstr.result.name, 0);
                    valStacks.put(allocaInstr.result.name, new Stack<>());
                }
                if (instr instanceof StoreInstr storeInstr && defs.containsKey(storeInstr.pointer.name))
                    defs.get(storeInstr.pointer.name).add(block);
            }
        }
    }
}
