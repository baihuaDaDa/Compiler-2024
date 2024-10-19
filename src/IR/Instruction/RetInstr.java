package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRLiteral;
import Util.IRObject.IREntity.IRLocalVar;

import java.util.HashSet;

public class RetInstr extends Instruction {
    public IREntity value;

    public RetInstr(IRBlock parent, IREntity value) {
        super(parent);
        this.value = value;
    }

    @Override
    public String toString() {
        if (value == null) return "ret void";
        return "ret " + value.type + " " + value;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public IRLocalVar getDef() {
        return null;
    }

    @Override
    public HashSet<IRLocalVar> getUse() {
        return new HashSet<>() {{
            if (value != null && value instanceof IRLocalVar localValue) add(localValue);
        }};
    }
}
