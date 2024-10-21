package Midend;

import IR.IRBlock;
import IR.IRProgram;
import IR.Instruction.CallInstr;
import IR.Instruction.Instruction;
import IR.Instruction.PhiInstr;
import IR.Module.FuncDefMod;
import Util.IRObject.IREntity.IRLocalVar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class DCE {
    private final IRProgram program;
    private final HashMap<IRLocalVar, HashSet<Instruction>> useList;
    private final HashSet<IRLocalVar> defList;
    private final HashMap<IRLocalVar, Instruction> defMap;

    public DCE(IRProgram program) {
        this.program = program;
        useList = new HashMap<>();
        defList = new HashSet<>();
        defMap = new HashMap<>();
    }

    public void run() {
        program.funcDefs.forEach(this::runFunc);
        if (program.initFunc != null) runFunc(program.initFunc);
        runFunc(program.mainFunc);
    }

    public void runFunc(FuncDefMod func) {
        HashSet<IRBlock> visited = new HashSet<>();
        ArrayList<IRBlock> ord = new ArrayList<>();
        Traverse(ord, visited, func.body.getFirst());
        // 此处直接删除了控制流图上后序遍历没有访问到的基本块
        // 注意此时基本块的标号 @blockNo 已经失效
        ArrayList<IRBlock> removeList = new ArrayList<>();
        for (var block : func.body)
            if (!ord.contains(block)) removeList.add(block);
        func.body.removeAll(removeList);
        // get uses and defs
        GetUseDef(func);
        // delete unused instructions
        while (!defList.isEmpty()) {
            var def = defList.iterator().next();
            var defInstr = defMap.get(def);
            defList.remove(def);
            var usedDef = useList.get(def);
            if (usedDef.isEmpty() && !(defInstr instanceof CallInstr)) {
                var uses = defInstr.getUse();
                for (var use : uses) {
                    useList.get(use).remove(defInstr);
                    defList.add(use);
                }
                defInstr.parent.instructions.remove(defInstr);
            }
        }
    }

    private void Traverse(ArrayList<IRBlock> ord, HashSet<IRBlock> visited, IRBlock block) {
        visited.add(block);
        for (var suc : block.suc)
            if (!visited.contains(suc)) Traverse(ord, visited, suc);
        ord.add(block);
    }

    private void GetUseDef(FuncDefMod func) {
        for (var block : func.body) {
            for (var instr : block.instructions) {
                if (instr.getDef() != null) {
                    defMap.put(instr.getDef(), instr);
                    defList.add(instr.getDef());
                    if (!useList.containsKey(instr.getDef())) useList.put(instr.getDef(), new HashSet<>());
                }
                var uses = instr.getUse();
                for (var use : uses) {
                    if (useList.containsKey(use)) useList.get(use).add(instr);
                    else useList.put(use, new HashSet<>(Set.of(instr)));
                }
            }
            for (var phiInstr : block.phiInstrs.values()) {
                defMap.put(phiInstr.getDef(), phiInstr);
                defList.add(phiInstr.getDef());
                if (!useList.containsKey(phiInstr.getDef())) useList.put(phiInstr.getDef(), new HashSet<>());
                var uses = phiInstr.getUse();
                for (var use : uses) {
                    if (useList.containsKey(use)) useList.get(use).add(phiInstr);
                    else useList.put(use, new HashSet<>(Set.of(phiInstr)));
                }
            }
        }
    }
}
