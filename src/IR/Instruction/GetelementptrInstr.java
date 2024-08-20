package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IREntity.IREntity;
import Util.IREntity.IRLocalVar;
import Util.IREntity.IRVariable;
import Util.Type.IRType;

import java.util.ArrayList;

public class GetelementptrInstr extends Instruction {
    public IRLocalVar result;
    public IRType type;
    public IRVariable pointer;
    public ArrayList<IREntity> indices;

    public GetelementptrInstr(IRBlock parent, IRLocalVar result, IRType type, IRVariable pointer) {
        super(parent);
        this.result = result;
        this.type = type;
        this.pointer = pointer;
        indices = new ArrayList<>();
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
}
