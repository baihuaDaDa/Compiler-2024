package Util.Scope;

import Util.Decl.ClassDecl;
import Util.Decl.FuncDecl;
import Util.Error.SemanticError;
import Util.Position;
import Util.Type.ReturnType;
import Util.Type.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GlobalScope extends Scope {
    public HashMap<String, ClassDecl> classes = null;
    public HashMap<String, FuncDecl> functions = null;

    public GlobalScope() {}

    public void defineClass(String className, Position pos) {
        if (classes.containsKey(className))
            throw new SemanticError("class redefine: " + className, pos);
        classes.put(className, new ClassDecl());
    }

    public void defineClassMember(String className, String memberName, Type type, Position pos) {
        if (classes.get(className).members.containsKey(memberName))
            throw new SemanticError("class member redefine: " + className + "." + memberName, pos);
        if ()
            throw new SemanticError("class function and member name duplicate: " + className + "." + memberName, pos);
        if (functions.containsKey(memberName))
            throw new SemanticError("function and class member name duplicate: " + memberName, pos);
        classes.get(className).members.put(memberName, type);
    }

    public void defineClassFunc(String className, String funcName, ArrayList<Type> paramTypes, ReturnType type, Position pos) {
        FuncDecl func = new FuncDecl(type, paramTypes);
        if (classes.get(className).methods.containsKey(funcName))
            throw new SemanticError("class function redefine: " + className + "." + funcName, pos);
        if (classes.get(className).members.containsKey(funcName))
            throw new SemanticError("class member and function name duplicate: " + className + "." + funcName, pos);
        if (functions.containsKey(funcName))
            throw new SemanticError("function and class function name duplicate: " + funcName, pos);
        classes.get(className).methods.put()
    }

    public void defineFunc(String name, FuncDecl decl) {

    }

    public FuncDecl getFunc(String name) {

        return null;
    }

    public boolean isFuncDefined(String name) {

        return false;
    }

    public boolean isClassDefined(String name) {

        return false;
    }
}
