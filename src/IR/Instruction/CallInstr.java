package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IREntity.IREntity;
import Util.IREntity.IRVariable;
import Util.Type.IRType;

import java.util.ArrayList;

public class CallInstr extends Instruction {
    public IRVariable result;
    public String funcName;
    public ArrayList<IREntity> args;

    public CallInstr(IRBlock parent, IRVariable result, String funcName, ArrayList<IREntity> args) {
        super(parent);
        this.result = result;
        this.funcName = funcName;
        this.args = args;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        if (result != null) ret.append(result).append(" = ");
        ret.append("call ");
        if (result == null) ret.append("void ");
        else ret.append(result.type);
        ret.append("@").append(funcName).append("(");
        for (int i = 0; i < args.size(); ++i) {
            ret.append(args.get(i));
            if (i != args.size() - 1) ret.append(", ");
        }
        ret.append(")");
        return ret.toString();
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
