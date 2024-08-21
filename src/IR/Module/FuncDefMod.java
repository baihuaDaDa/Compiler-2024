package IR.Module;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IREntity.IRLocalVar;
import Util.Type.IRType;

import java.util.ArrayList;

public class FuncDefMod extends Module {
    public IRType returnType;
    public String funcName;
    public ArrayList<IRLocalVar> params;
    public ArrayList<IRBlock> body;

    public FuncDefMod(IRType returnType, String funcName) {
        this.returnType = returnType;
        this.funcName = funcName;
        params = new ArrayList<>();
        body = new ArrayList<>();
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("define ").append(returnType).append(" @").append(funcName).append("(");
        for (int i = 0; i < params.size(); ++i) {
            ret.append(params.get(i).type).append(" ").append(params.get(i));
            if (i != params.size() - 1)
                ret.append(", ");
        }
        ret.append(") {\n");
        for (IRBlock block : body) {
            ret.append(block);
        }
        ret.append("}\n");
        return ret.toString();
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
