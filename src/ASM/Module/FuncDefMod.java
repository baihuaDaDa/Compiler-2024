package ASM.Module;

import ASM.ASMSection;
import Util.PhysicalReg;

import java.util.ArrayList;
import java.util.HashMap;

public class FuncDefMod extends Module {
    public ArrayList<Block> body;
    public HashMap<String, Integer> paramMap;
    public int argCnt = 0, stackSize = 0;
    // arguments 中前八个用于暂存当前函数体的 a0-a7，剩下的用于存储调用函数的第 9 个及以后的参数
    // argCnt 代表需要保存的寄存器数量

    // 寄存器分配后需存储形参、溢出变量、caller保存、callee保存
    public HashMap<String, PhysicalReg> regMap;
    public HashMap<String, Integer> spilledVarMap;
    public int spilledVarCnt = 0, callerSaveCnt = 0, calleeSaveCnt = 0;
    // 8 args to save (current func) --- more args to save (called func) --- spilled variables --- caller save --- callee save --- ra

    public FuncDefMod(ASMSection parent, IR.Module.FuncDefMod mod) {
        super(parent, mod.funcName);
        body = new ArrayList<>();
        paramMap = new HashMap<>();
        var entryBlock = new Block(this, label);
        body.add(entryBlock);
        mod.body.getFirst().asmBlock = entryBlock;
    }

    public void addBlock(Block block) {
        body.add(block);
    }

    public boolean isParamReg(String name) {
        return paramMap.containsKey(name);
    }

    public boolean isPhysicalReg(String name) {
        return regMap.containsKey(name);
    }

    public boolean isSpilledVar(String name) {
        return spilledVarMap.containsKey(name);
    }

    public int getParamReg(String name) {
        return paramMap.get(name) * 4 + stackSize;
    }

    public PhysicalReg getReg(String name) {
        return regMap.get(name);
    }

    public int getSpilledVar(String name) {
        return (spilledVarMap.get(name) + argCnt) * 4;
    }

    public int getArgReg(int no) {
        return no * 4;
    }

    public int getCallerSaveHead() {
        return (argCnt + spilledVarCnt) * 4;
    }

    public int getCalleeSaveHead() {
        return (argCnt + spilledVarCnt + callerSaveCnt) * 4;
    }

    public int getRetReg() {
        return stackSize - 4;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(String.format("\t%-8s", ".globl")).append(label).append("\n");
        for (Block block : body)
            ret.append(block);
        ret.append("\n");
        return ret.toString();
    }
}
