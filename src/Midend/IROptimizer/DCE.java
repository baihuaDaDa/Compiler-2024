package Midend.IROptimizer;

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
                var uses = defInstr.getUse();
                for (var use : uses) {
                    useList.get(use).remove(defInstr);
                    defList.add(use);
                }
                if (!defInstr.parent.instructions.remove(defInstr))
                    defInstr.parent.phiInstrs.remove(((PhiInstr) defInstr).originalName);
            }
        }
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
