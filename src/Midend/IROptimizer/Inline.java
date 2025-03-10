package Midend.IROptimizer;

import IR.IRBlock;
import IR.IRProgram;
import IR.Instruction.*;
import IR.Module.FuncDefMod;
import Midend.IROptimizer.Util.IRCFGBuilder;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRLiteral;
import Util.IRObject.IREntity.IRLocalVar;
import Util.IRObject.IREntity.IRVariable;

import java.util.ArrayList;
import java.util.HashMap;

public class Inline {
    private final IRProgram program;
    private final int inlineTimes;
    private final HashMap<CallInstr, InlineFunc> inlineMap;
    private final HashMap<String, FuncDefMod> funcDefMap;
    private final HashMap<IRBlock, IRBlock> replaceMap;

    public Inline(IRProgram program, int inlineTimes) {
        this.program = program;
        this.inlineTimes = inlineTimes;
        this.inlineMap = new HashMap<>();
        this.funcDefMap = new HashMap<>();
        this.replaceMap = new HashMap<>();
    }

    public void run() {
        // .init needn't inline
        for (var func : program.funcDefs) funcDefMap.put(func.funcName, func);
        for (int i = 0; i < inlineTimes; i++) {
            program.funcDefs.forEach(this::getInlineFunc);
            getInlineFunc(program.mainFunc);
            program.funcDefs.forEach(this::insertInlineFunc);
            insertInlineFunc(program.mainFunc);
            IRCFGBuilder irCFGBuilder = new IRCFGBuilder(program);
            irCFGBuilder.build(); // update CFG after every inline operation
        }
    }

    private void getInlineFunc(FuncDefMod func) {
        for (var block : func.body)
            for (var instr : block.instructions) {
                if (instr instanceof CallInstr callInstr) {
                    boolean flag = false;
                    for (var arg : callInstr.args)
                        if (arg instanceof IRLiteral irLiteral && irLiteral.isNull) {
                            flag = true;
                            break;
                        } // 不能内联含有null参数的调用语句，会导致有关指针的指令出现null指针异常
                    if (!flag && funcDefMap.containsKey(callInstr.funcName)) {
                        inlineMap.put(callInstr, new InlineFunc(callInstr, funcDefMap.get(callInstr.funcName)));
                        replaceMap.put(callInstr.parent, inlineMap.get(callInstr).exitBlock); // 替换至最后一个exit块
                    }
                }
            }
    }

    private void insertInlineFunc(FuncDefMod func) {
        for (int k = 0; k < func.body.size(); k++) {
            IRBlock block = func.body.get(k);
            int i = 0;
            while (i < block.instructions.size()) {
                if (block.instructions.get(i) instanceof CallInstr callInstr && inlineMap.containsKey(callInstr)) {
                    InlineFunc inlineFunc = inlineMap.get(callInstr);
                    func.body.add(k + 1, inlineFunc.exitBlock);
                    func.body.addAll(k + 1, inlineFunc.body);
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
        // 更新原本块的后继的phi的前驱为新的exit块
        for (var block : func.body)
            for (var pred : block.pred)
                if (replaceMap.containsKey(pred)) {
                    IRBlock newPred = replaceMap.get(pred);
                    for (var phiInstr : block.phiInstrs.values()) phiInstr.changeBlock(newPred, pred);
                }
    }

    private static class InlineFunc {
        public final FuncDefMod target;
        public final ArrayList<IRBlock> body;
        public IRBlock exitBlock;
        private final HashMap<IRLocalVar, IREntity> localVarMap;
        private final HashMap<IRBlock, IRBlock> blockMap;
        private int inlineCnt = 0;
        private static final HashMap<FuncDefMod, Integer> inlineCntMap = new HashMap<>();

        public InlineFunc(CallInstr callInstr, FuncDefMod inlineFunc) {
            this.target = inlineFunc;
            this.body = new ArrayList<>();
            this.localVarMap = new HashMap<>();
            this.blockMap = new HashMap<>();
            FuncDefMod parent = callInstr.parent.parent;
            // 函数被内联了几次
            if (inlineCntMap.containsKey(target)) {
                inlineCnt = inlineCntMap.get(target);
                inlineCntMap.put(target, inlineCnt + 1);
            } else inlineCntMap.put(target, 1);
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
                for (var phiInstr : block.phiInstrs.values())
                    newBlock.addPhiInstr((PhiInstr) ReplaceInlineVars(phiInstr, newBlock));
                for (var instr : block.instructions) {
                    if (instr instanceof RetInstr retInstr) {
                        if (retInstr.value != null)
                            exitBlock.phiInstrs.get(callInstr.result.name).addBranch(RenameInlineVar(retInstr.value), newBlock);
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
