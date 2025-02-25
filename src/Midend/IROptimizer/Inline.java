package Midend.IROptimizer;

import IR.IRBlock;
import IR.IRProgram;
import IR.Instruction.*;
import IR.Module.FuncDefMod;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRLocalVar;
import Util.IRObject.IREntity.IRVariable;

import java.util.ArrayList;
import java.util.HashMap;

public class Inline {
    private final IRProgram program;
    private final int inlineTimes;
    private final HashMap<CallInstr, InlineFunc> inlineMap;
    private final HashMap<String, FuncDefMod> funcDefs;

    public Inline(IRProgram program, int inlineTimes) {
        this.program = program;
        this.inlineTimes = inlineTimes;
        this.inlineMap = new HashMap<>();
        this.funcDefs = new HashMap<>();
    }

    public void run() {
        for (var func : program.funcDefs) funcDefs.put(func.funcName, func);
        for (int i = 0; i < inlineTimes; i++) {
            program.funcDefs.forEach(this::getInlineFunc);
            program.funcDefs.forEach(this::insertInlineFunc);
        }
    }

    private void getInlineFunc(FuncDefMod func) {
        for (var block : func.body)
            for (var instr : block.instructions)
                if (instr instanceof CallInstr callInstr)
                    inlineMap.put(callInstr, new InlineFunc(callInstr, funcDefs.get(callInstr.funcName)));
    }

    private void insertInlineFunc(FuncDefMod func) {
        for (int k = 0; k < func.body.size(); k++) {
            IRBlock block = func.body.get(k);
            int i = 0;
            while (i < block.instructions.size()) {
                if (block.instructions.get(i) instanceof CallInstr callInstr) {
                    InlineFunc inlineFunc = inlineMap.get(callInstr);
                    func.body.addAll(k + 1, inlineFunc.body);
                    func.body.add(k + 1, inlineFunc.exitBlock);
                    for (int j = i + 1; j < block.instructions.size(); block.instructions.remove(j)) {
                        Instruction mvInstr = block.instructions.get(j);
                        inlineFunc.exitBlock.addInstr(mvInstr);
                        mvInstr.parent = inlineFunc.exitBlock;
                    }
                    block.instructions.remove(i);
                    block.addInstr(i, new BrInstr(block, null, inlineFunc.body.getFirst(), null));
                    k = func.body.indexOf(inlineFunc.exitBlock);
                    block = inlineFunc.exitBlock;
                    i = 0;
                } else i++;
            }
        }
    }

    private static class InlineFunc {
        public final FuncDefMod target;
        public final ArrayList<IRBlock> body;
        public IRBlock exitBlock;
        private final HashMap<IRLocalVar, IREntity> localVarMap;
        private final HashMap<IRBlock, IRBlock> blockMap;
        private int inlineCnt = 0;

        public InlineFunc(CallInstr callInstr, FuncDefMod inlineFunc) {
            this.target = inlineFunc;
            this.body = new ArrayList<>();
            this.localVarMap = new HashMap<>();
            this.blockMap = new HashMap<>();
            FuncDefMod parent = callInstr.parent.parent;
            // 函数被内联了几次
            if (parent.inlineFuncCnt.containsKey(target)) {
                inlineCnt = parent.inlineFuncCnt.get(target);
                parent.inlineFuncCnt.put(target, inlineCnt + 1);
            } else parent.inlineFuncCnt.put(target, 1);
            // 创建出口块
            exitBlock = new IRBlock(parent, String.format("inline%d.%s.exit", inlineCnt, target.funcName));
            if (!inlineFunc.returnType.isVoid) exitBlock.addPhiInstr(new PhiInstr(exitBlock, callInstr.result));
            // 映射表
            // 函数形参替换为caller传入的参数
            for (var param : target.params)
                localVarMap.put(param, callInstr.args.get(target.params.indexOf(param)));
            // 复制函数体
            for (var block : target.body) {
                IRBlock newBlock = new IRBlock(parent, String.format("inline%d.%s.%s", inlineCnt, target.funcName, block.label));
                body.add(newBlock);
                blockMap.put(block, newBlock);
            } // 先更新所有块标签
            for (var block : target.body) {
                IRBlock newBlock = blockMap.get(block);
                for (var phiInstr : block.phiInstrs.values()) newBlock.addPhiInstr((PhiInstr) ReplaceInlineVars(phiInstr, newBlock));
                for (var instr : block.instructions) {
                    if (instr instanceof RetInstr retInstr) {
                        if (retInstr.value != null) exitBlock.phiInstrs.get(callInstr.result.name).addBranch(retInstr.value, newBlock);
                        newBlock.addInstr(new BrInstr(newBlock, null, exitBlock, null));
                    } else newBlock.addInstr(ReplaceInlineVars(instr, newBlock));
                }
            }
        }

        private IREntity RenameInlineVar(IREntity origin) {
            if (origin == null) return null;
            else if (origin instanceof IRLocalVar localOrigin) {
                if (localVarMap.containsKey(localOrigin)) return localVarMap.get(localOrigin);
                else {
                    IRLocalVar newVar = new IRLocalVar(String.format("inline%d.%s.%s", inlineCnt, target.funcName, localOrigin.name), localOrigin.type);
                    localVarMap.put(localOrigin, newVar);
                    return newVar;
                }
            } else return origin;
        }

        private Instruction ReplaceInlineVars(Instruction instr, IRBlock newParent) {
            Instruction newInstr;
            switch (instr) {
                case BinaryInstr binaryInstr ->
                        newInstr = new BinaryInstr(newParent, binaryInstr.op, RenameInlineVar(binaryInstr.lhs), RenameInlineVar(binaryInstr.rhs), (IRLocalVar) RenameInlineVar(binaryInstr.result));
                case BrInstr brInstr ->
                        newInstr = new BrInstr(newParent, (IRLocalVar) RenameInlineVar(brInstr.cond), blockMap.get(brInstr.thenBlock), blockMap.get(brInstr.elseBlock));
                case CallInstr callInstr -> {
                    newInstr = new CallInstr(newParent, (IRLocalVar) RenameInlineVar(callInstr.result), callInstr.funcName, new ArrayList<>());
                    for (var arg : callInstr.args)
                        ((CallInstr) newInstr).args.add(RenameInlineVar(arg));
                }
                case GetelementptrInstr getelementptrInstr -> {
                    newInstr = new GetelementptrInstr(newParent, (IRLocalVar) RenameInlineVar(getelementptrInstr.result),
                            getelementptrInstr.type, (IRVariable) RenameInlineVar(getelementptrInstr.pointer));
                    for (var index : getelementptrInstr.indices)
                        ((GetelementptrInstr) newInstr).indices.add(RenameInlineVar(index));
                }
                case IcmpInstr icmpInstr -> newInstr = new IcmpInstr(newParent, icmpInstr.cond,
                        RenameInlineVar(icmpInstr.lhs), RenameInlineVar(icmpInstr.rhs),
                        (IRLocalVar) RenameInlineVar(icmpInstr.result));
                case LoadInstr loadInstr -> newInstr = new LoadInstr(newParent,
                        (IRLocalVar) RenameInlineVar(loadInstr.result), (IRVariable) RenameInlineVar(loadInstr.pointer));
                case PhiInstr phiInstr -> {
                    newInstr = new PhiInstr(newParent, (IRLocalVar) RenameInlineVar(phiInstr.result));
                    for (var pair : phiInstr.pairs)
                        ((PhiInstr) newInstr).addBranch(RenameInlineVar(pair.a), blockMap.get(pair.b));
                }
                case StoreInstr storeInstr -> newInstr = new StoreInstr(newParent,
                        RenameInlineVar(storeInstr.value), (IRVariable) RenameInlineVar(storeInstr.pointer));
                default -> throw new RuntimeException("Unsupported instruction in inline");
            }
            return newInstr;
        }
    }
}
