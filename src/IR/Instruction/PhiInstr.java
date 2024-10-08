package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRLocalVar;
import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;

public class PhiInstr extends Instruction {
    public IRLocalVar result;
    public ArrayList<Pair<IREntity, IRBlock>> pairs = null;

    public PhiInstr(IRBlock parent, IRLocalVar result) {
        super(parent);
        this.result = result;
        pairs = new ArrayList<>();
    }

    public void addBranch(IREntity value, IRBlock block) {
        pairs.add(new Pair<>(value, block));
    }

    public void changeValue(IREntity value, IRBlock block) {
        pairs.removeIf(pair -> pair.b == block);
        addBranch(value, block);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(result).append(" = phi ").append(result.type).append(" ");
        for (var pair : pairs) {
            ret.append("[ ").append(pair.a).append(", %").append(pair.b.label).append(" ]");
            if (pairs.indexOf(pair) != pairs.size() - 1)
                ret.append(", ");
        }
        return ret.toString();
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
