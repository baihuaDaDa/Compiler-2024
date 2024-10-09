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

import java.util.*;

public class Mem2Reg {
    private IRProgram program;
    private HashMap<String, IRType> allocVars;
    private HashMap<String, HashSet<IRBlock>> defs;
    private HashMap<String, Integer> nums;
    private HashMap<String, Stack<IREntity>> valStacks;

    public Mem2Reg(IRProgram program) {
        this.program = program;
        this.allocVars = new HashMap<>();
        this.defs = new HashMap<>();
        this.nums = new HashMap<>();
        this.valStacks = new HashMap<>();
    }

    public void run() {
        DomTreeBuilder domTreeBuilder = new DomTreeBuilder(program);
        domTreeBuilder.build();
        for (var func : program.funcDefs) {
            collectAllocVarsAndDefs(func);
            placePhi(func);
            rename(func.body.getFirst());
            reinit();
        }
        if (program.initFunc != null) {
            collectAllocVarsAndDefs(program.initFunc);
            placePhi(program.initFunc);
            rename(program.initFunc.body.getFirst());
            reinit();
        }
        collectAllocVarsAndDefs(program.mainFunc);
        placePhi(program.mainFunc);
        rename(program.mainFunc.body.getFirst());
    }

    private void reinit() {
        allocVars.clear();
        defs.clear();
        nums.clear();
        valStacks.clear();
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
            if (instr instanceof AllocaInstr) removeList.add(instr);
            else if (instr instanceof StoreInstr storeInstr) {
                if (storeInstr.pointer instanceof IRLocalVar && valStacks.containsKey(storeInstr.pointer.name)) {
                    var stack = valStacks.get(storeInstr.pointer.name);
                    if (!(storeInstr.value instanceof IRLiteral) || !storeInstr.value.type.isSameType(new IRType("ptr")))
                        stack.push(storeInstr.value);
                    removeList.add(instr);
                }
            } else if (instr instanceof LoadInstr loadInstr) {
                if (loadInstr.pointer instanceof IRLocalVar && valStacks.containsKey(loadInstr.pointer.name)) {
                    var stack = valStacks.get(loadInstr.pointer.name);
                    if (stack.empty()) {
                        removeList.add(instr);
                        continue;
                    }
                    if (stack.peek() instanceof IRLocalVar localVar) {
                        loadInstr.result.name = localVar.name;
                        loadInstr.result.type = localVar.type;
                        removeList.add(instr);
                    } else if (stack.peek() instanceof IRLiteral literal) {
                        var x0 = new IRLiteral(literal.type, 0);
                        // TODO 有待改进，有不那么唐氏的做法
                        instrs.set(instrs.indexOf(instr), new BinaryInstr(block, "add", literal, x0, loadInstr.result));
                    } else if (stack.peek() instanceof IRGlobalPtr globalPtr) {
                        if (!globalPtr.name.startsWith(".str.")) throw new RuntimeException("Unexpected global pointer type in `valStack`(not const string)");
                        // TODO 有待改进，有不那么唐氏的做法
                        instrs.set(instrs.indexOf(instr), new GetelementptrInstr(block, loadInstr.result, "i32", globalPtr, new IRLiteral(new IRType("i32"), 0)));
                    } else throw new RuntimeException("Unexpected type in `valStack`");
                }
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
