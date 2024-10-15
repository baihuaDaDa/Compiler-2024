package IR.Module;

import Util.PhysicalReg;
import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IRLocalVar;
import Util.Type.IRType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class FuncDefMod extends Module {
    public IRType returnType;
    public String funcName;
    public ArrayList<IRLocalVar> params;
    public ArrayList<IRBlock> body;
    public HashMap<String, Integer> localVars;
    public int anonymousVarCnt = 0;
    public int dotICnt = 0; // for NewEmptyArray

    // for linear scanner
    public HashSet<IRLocalVar> spilledVars;
    public HashMap<IRLocalVar, PhysicalReg> regMap;

    public FuncDefMod(IRType returnType, String funcName, ArrayList<IRLocalVar> params) {
        this.returnType = returnType;
        this.funcName = funcName;
        this.params = params;
        body = new ArrayList<>();
        localVars = new HashMap<>();
        addBlock(new IRBlock(this, "entry"));
        spilledVars = new HashSet<>();
        regMap = new HashMap<>();
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

    public void addBlock(IRBlock block) {
        body.add(block);
    }
}
