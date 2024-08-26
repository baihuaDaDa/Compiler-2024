package ASM.Module;

import ASM.ASMSection;

import java.util.ArrayList;

public class FuncDefMod extends Module {
    public ArrayList<Block> body;

    public FuncDefMod(ASMSection parent, String label) {
        super(parent, label);
        body = new ArrayList<>();
        body.add(new Block(this, label));
    }

    public void addBlock(Block block) {
        body.add(block);
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
