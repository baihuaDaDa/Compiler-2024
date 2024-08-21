package Util.Scope;

import IR.IRBlock;

import java.util.HashMap;

public class IRScope {
    public IRScope parent;
    public HashMap<String, Integer> vars;
    public String className;
    public IRBlock loopEnd;
    public IRBlock loopStep;

    public IRScope(IRScope parent) {
        this.parent = parent;
        this.vars = new HashMap<>();
    }

    public void defineVar(String name, int no) {
        vars.put(name, no);
    }

    public int getVarNo(String name) {
        return vars.get(name);
    }
}
