package ASM.Module;

import ASM.ASMSection;

import java.util.ArrayList;
import java.util.HashMap;

public class FuncDefMod extends Module {
    public ArrayList<Block> body;
    public HashMap<String, Integer> virtualRegMap;
    public HashMap<String, Integer> allocPtrMap;
    public int virtualRegCnt = 0, allocCnt = 0, argCnt = 0, stackSize = 0;

    public FuncDefMod(ASMSection parent, String label) {
        super(parent, label);
        body = new ArrayList<>();
        virtualRegMap = new HashMap<>();
        allocPtrMap = new HashMap<>();
        body.add(new Block(this, label));
    }

    public void addBlock(Block block) {
        body.add(block);
    }

    public int getArgReg(int no) {
        return no * 4;
    }

    public int getAllocPtr(String name) {
        return (allocPtrMap.get(name) + argCnt) * 4;
    }

    public int getVirtualReg(String name) {
        return (virtualRegMap.get(name) + allocCnt + argCnt) * 4;
    }

    public int getRetReg() {
        return stackSize - 4;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(String.format("\t%-8s", ".globl")).append(label);
        for (Block block : body)
            ret.append(block);
        ret.append("\n");
        return ret.toString();
    }
}
