package Util.Scope;

import Util.Error.SemanticError;
import Util.Position;
import Util.Type.Type;

import java.util.HashMap;

public class Scope {
    private HashMap<String, Type> vars = null;
    private Scope parent = null;

    public Scope(Scope parent) {
        this.parent = parent;
        this.vars = new HashMap<>();
    }

    public void defineVar(String name, Type type, Position pos) {
        if (vars.containsKey(name))
            throw new SemanticError("variable redefine: " + name, pos);
        vars.put(name, type);
    }

    public Type getVar(String name) {
        if (vars.containsKey(name)) return vars.get(name);
        else if (parent != null) return parent.getVar(name);
        else return null;
    }

    public boolean isVarDefined(String name) {
        return vars.containsKey(name) || (parent != null && parent.isVarDefined(name));
    }
}