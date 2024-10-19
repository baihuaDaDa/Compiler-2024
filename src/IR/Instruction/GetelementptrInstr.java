package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRLocalVar;
import Util.IRObject.IREntity.IRVariable;
import Util.Type.IRType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class GetelementptrInstr extends Instruction {
    public IRLocalVar result;
    public String type;
    public IRVariable pointer;
    public ArrayList<IREntity> indices;

    public GetelementptrInstr(IRBlock parent, IRLocalVar result, String type, IRVariable pointer, IREntity ... indices) {
        super(parent);
        this.result = result;
        this.type = type;
        this.pointer = pointer;
        this.indices = new ArrayList<>();
        Collections.addAll(this.indices, indices);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(result).append(" = getelementptr ").append(type);
        ret.append(", ptr ").append(pointer);
        for (IREntity index : indices)
            ret.append(", ").append(index.type).append(" ").append(index);
        return ret.toString();
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public IRLocalVar getDef() {
        return result;
    }

    @Override
    public HashSet<IRLocalVar> getUse() {
        HashSet<IRLocalVar> ret = new HashSet<>();
        if (pointer instanceof IRLocalVar localPointer) ret.add(localPointer);
        for (IREntity index : indices)
            if (index instanceof IRLocalVar localIndex) ret.add(localIndex);
        return ret;
    }
}
