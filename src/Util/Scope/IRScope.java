package Util.Scope;

import IR.IRBlock;

import java.util.HashMap;

public class IRScope {
    public IRScope parent;
    public HashMap<String, Integer> vars;
    public String className;
    public IRBlock loopEnd; // for break in for&while-statement
    public IRBlock loopNext; // for continue in for&while-statement

    public IRScope(IRScope parent) { // common
        this.parent = parent;
        this.vars = new HashMap<>();
        this.className = parent.className;
        this.loopEnd = parent.loopEnd;
        this.loopNext = parent.loopNext;
    }

    public IRScope(IRScope parent, String className) { // for classScope
        this.parent = parent;
        this.className = className;
        this.vars = new HashMap<>();
    }

    public IRScope(IRScope parent, IRBlock loopEnd, IRBlock loopNext) { // for for&while-statement
        this.parent = parent;
        this.vars = new HashMap<>();
        this.className = parent.className;
        this.loopEnd = loopEnd;
        this.loopNext = loopNext;
    }

    public void addVar(String name, int no) {
        vars.put(name, no);
    }

    public int getVarNo(String name) {
        if (vars.containsKey(name)) return vars.get(name);
        else if (parent != null) return parent.getVarNo(name);
        else return 0;
    }
}
