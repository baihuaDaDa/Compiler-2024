package IR.Module;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IREntity.IRLocalVar;
import Util.Type.IRType;

import java.util.ArrayList;
import java.util.HashMap;

public class FuncDefMod extends Module {
    public IRType returnType;
    public String funcName;
    public ArrayList<IRLocalVar> params;
    public ArrayList<IRBlock> body;
    public HashMap<String, Integer> localVars;
    public int anonymousVarCnt = 0, loopCnt = 0, ifCnt = 0;

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

    public void addLocalVar(String name, int no) {
        localVars.put(name, no);
    }

    public int getLocalVarNo(String name) {
        if (!localVars.containsKey(name)) {
            localVars.put(name, 0);
            return 0;
        }
        return localVars.get(name);
    }
}
