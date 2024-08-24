package IR.Module;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IRLocalVar;
import Util.Type.IRType;

import java.util.ArrayList;
import java.util.HashMap;

public class FuncDefMod extends Module {
    public IRType returnType;
    public String funcName;
    public ArrayList<IRLocalVar> params;
    public ArrayList<IRBlock> body;
    public HashMap<String, Integer> localVars;
    public int anonymousVarCnt = 0, loopCnt = 0, ifCnt = 0, ternaryCnt = 0, andOrCnt = 0;

    public FuncDefMod(IRType returnType, String funcName, ArrayList<IRLocalVar> params) {
        this.returnType = returnType;
        this.funcName = funcName;
        this.params = params;
        body = new ArrayList<>();
        body.add(new IRBlock(this, "entry"));
        localVars = new HashMap<>();
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

    public void defineLocalVar(String name, int no) {
        localVars.put(name, no);
    }

    public int getLocalVarNo(String name) {
        return localVars.getOrDefault(name, 0);
    }
}
