package Midend;

import IR.IRProgram;
import IR.Instruction.CallInstr;
import IR.Instruction.Instruction;
import IR.Instruction.PhiInstr;
import IR.Module.FuncDefMod;
import Util.IRObject.IREntity.IRLocalVar;

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
        // get uses and defs
        GetUseDef(func);
        // delete unused instructions
        while (!defList.isEmpty()) {
            var def = defList.iterator().next();
            var defInstr = defMap.get(def);
            defList.remove(def);
            var usedDef = useList.get(def);
            if (usedDef.isEmpty() && !(defInstr instanceof CallInstr)) {
                if (!(defInstr instanceof PhiInstr)) {
                    var uses = func.useMap.get(defInstr);
                    for (var use : uses) {
                        useList.get(use).remove(defInstr);
                        defList.add(use);
                    }
                }
                defInstr.parent.instructions.remove(defInstr);
                func.useMap.remove(defInstr);
                func.defMap.remove(defInstr);
                func.inMap.remove(defInstr);
                func.outMap.remove(defInstr);
            }
        }
    }

    private void GetUseDef(FuncDefMod func) {
        for (var block : func.body)
            for (var instr : block.instructions) {
                func.useMap.put(instr, instr.getUse());
                func.defMap.put(instr, instr.getDef());
                func.outMap.put(instr, new HashSet<>());
                func.inMap.put(instr, new HashSet<>(func.useMap.get(instr)));
                if (instr.getDef() != null) {
                    defMap.put(instr.getDef(), instr);
                    defList.add(instr.getDef());
                }
                if (!useList.containsKey(instr.getDef())) useList.put(instr.getDef(), new HashSet<>());
                var uses = func.useMap.get(instr);
                for (var use : uses) {
                    if (useList.containsKey(use)) useList.get(use).add(instr);
                    else useList.put(use, new HashSet<>(Set.of(instr)));
                }
            }
        for (var block : func.body)
            for (var phiInstr : block.phiInstrs.values()) {
                func.defMap.put(phiInstr, phiInstr.getDef());
                func.outMap.put(phiInstr, new HashSet<>());
                func.inMap.put(phiInstr, new HashSet<>());
                defMap.put(phiInstr.getDef(), phiInstr);
                defList.add(phiInstr.getDef());
                if (!useList.containsKey(phiInstr.getDef())) useList.put(phiInstr.getDef(), new HashSet<>());
                for (var branch : phiInstr.pairs)
                    if (branch.a instanceof IRLocalVar localVar) {
                        func.useMap.get(branch.b.instructions.getLast()).add(localVar); // phi 的 use 在控制语句之前的一条语句
                        if (useList.containsKey(localVar)) useList.get(localVar).add(branch.b.instructions.getLast());
                        else useList.put(localVar, new HashSet<>(Set.of(branch.b.instructions.getLast())));
                    }
            }
    }
}