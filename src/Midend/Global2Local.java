package Midend;

import IR.IRProgram;
import IR.Instruction.*;
import IR.Module.FuncDefMod;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRGlobalPtr;
import Util.IRObject.IREntity.IRLocalVar;
import Util.Type.IRType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Global2Local {
    private final IRProgram program;
    private final HashSet<FuncDefMod> visited;
    private final ArrayList<FuncDefMod> ord;
    private final ArrayList<SCCNode> dag;
    private final HashMap<FuncDefMod, SCCNode> colorMap;
    private final HashMap<String, FuncDefMod> funcDefs;

    private static class SCCNode {
        public HashSet<FuncDefMod> funcDefs;
        public HashSet<SCCNode> predecessors;
        public HashSet<SCCNode> successors;
        public HashSet<IRGlobalPtr> loadList;
        public HashSet<IRGlobalPtr> storeList;

        public SCCNode() {
            funcDefs = new HashSet<>();
            predecessors = new HashSet<>();
            successors = new HashSet<>();
            loadList = new HashSet<>();
            storeList = new HashSet<>();
        }
    }

    public Global2Local(IRProgram program) {
        this.program = program;
        visited = new HashSet<>();
        ord = new ArrayList<>();
        dag = new ArrayList<>();
        funcDefs = new HashMap<>();
        colorMap = new HashMap<>();
    }

    public void run() {
        for (var func : program.funcDefs) funcDefs.put(func.funcName, func);
        if (program.initFunc != null) funcDefs.put(program.initFunc.funcName, program.initFunc);
        funcDefs.put(program.mainFunc.funcName, program.mainFunc);
        // Kosaraju 强连通缩点
        PostOrderDFS(program.mainFunc);
        program.funcDefs.removeIf(func -> !ord.contains(func)); // delete unused functions
        for (int i = ord.size() - 1; i >= 0; --i) {
            var func = ord.get(i);
            if (!colorMap.containsKey(func)) {
                var scc = new SCCNode();
                dag.add(scc);
                SecondDFS(func, scc);
            }
        }
        // find loads and stores of global pointers
        for  (var func : ord) {
            var sccNode = colorMap.get(func);
            for (var block : func.body)
                for (var instr : block.instructions) {
                    if (instr instanceof LoadInstr loadInstr && loadInstr.pointer instanceof IRGlobalPtr globalPtr)
                        sccNode.loadList.add(globalPtr);
                    else if (instr instanceof StoreInstr storeInstr && storeInstr.pointer instanceof IRGlobalPtr globalPtr)
                        sccNode.storeList.add(globalPtr);
                }
        }
        // localize const global pointers
        for (var scc : dag)
            for (var globalPtr : scc.loadList)
                // TODO 算法可以优化，搜到一个可以 localize 的就说明它的后继都可以 localize
                if (ConstantDFS(scc, globalPtr))
                    for (var func : scc.funcDefs) {
                        IRLocalVar global2local = null;
                        HashMap<IRLocalVar, IREntity> renameMap = new HashMap<>();
                        for (var block : func.body) {
                            ArrayList<Instruction> removeList = new ArrayList<>();
                            for (var instr : block.instructions) {
                                if (instr instanceof LoadInstr loadInstr && loadInstr.pointer instanceof IRGlobalPtr) {
                                    if (global2local == null) global2local = loadInstr.result;
                                    else {
                                        renameMap.put(loadInstr.result, global2local);
                                        removeList.add(instr);
                                    }
                                } else instr.rename(renameMap);
                            }
                            block.instructions.removeAll(removeList);
                        }
                    }

        // localize variable global pointers
        for (var scc : dag) {
            if (scc.funcDefs.size() > 1) continue;
            for (var globalPtr : scc.storeList)
                if (VariableDFS(scc, globalPtr)) {
                    var func = scc.funcDefs.iterator().next();
                    var global2local = new IRLocalVar(String.format("gVar.%s", globalPtr.name), new IRType("ptr"));
                    func.body.getFirst().instructions.addFirst(new AllocaInstr(func.body.getFirst(), global2local, globalPtr.type));
                    for (var block : func.body)
                        for (var instr : block.instructions) {
                            if (instr instanceof LoadInstr loadInstr && loadInstr.pointer instanceof IRGlobalPtr)
                                loadInstr.pointer = global2local;
                            if (instr instanceof StoreInstr storeInstr && storeInstr.pointer instanceof IRGlobalPtr)
                                storeInstr.pointer = global2local;
                        }
                }
        }
    }

    private void PostOrderDFS(FuncDefMod cur) {
        visited.add(cur);
        for (var block : cur.body)
            for (var instr : block.instructions)
                if (instr instanceof CallInstr callInstr && funcDefs.containsKey(callInstr.funcName)) {
                    var calling = funcDefs.get(callInstr.funcName);
                    calling.callers.add(cur);
                    cur.callings.add(calling);
                    PostOrderDFS(calling);
                }
        ord.add(cur);
    }

    private void SecondDFS(FuncDefMod cur, SCCNode scc) {
        scc.funcDefs.add(cur);
        colorMap.put(cur, scc);
        for (var calling : cur.callings) {
            if (!colorMap.containsKey(calling)) SecondDFS(calling, scc);
            else {
                scc.successors.add(colorMap.get(calling));
                colorMap.get(calling).predecessors.add(scc);
            }
        }
    }

    private boolean ConstantDFS(SCCNode cur, IRGlobalPtr target) {
        boolean ret = !cur.storeList.contains(target);
        for (var suc : cur.successors) ret &= ConstantDFS(suc, target);
        return ret;
    }

    private boolean VariableDFS(SCCNode cur, IRGlobalPtr target) {
        boolean ret = !cur.loadList.contains(target) && !cur.storeList.contains(target);
        for (var suc : cur.successors) ret &= VariableDFS(suc, target);
        return ret;
    }
}
