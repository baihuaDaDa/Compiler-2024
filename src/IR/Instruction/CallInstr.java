package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRLocalVar;
import Util.IRObject.IREntity.IRVariable;

import java.util.ArrayList;
import java.util.HashSet;

public class CallInstr extends Instruction {
    public IRLocalVar result;
    public String funcName;
    public ArrayList<IREntity> args;

    public CallInstr(IRBlock parent, IRLocalVar result, String funcName, ArrayList<IREntity> args) {
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
        else ret.append(result.type).append(" ");
        ret.append("@").append(funcName).append("(");
        for (int i = 0; i < args.size(); ++i) {
            ret.append(args.get(i).type).append(" ").append(args.get(i));
            if (i != args.size() - 1) ret.append(", ");
        }
        ret.append(")");
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
        return new HashSet<>() {{
            for (IREntity arg : args)
                if (arg instanceof IRLocalVar localArg) add(localArg);
        }};
    }
}
