package Util.Scope;

import Util.Decl.ClassDecl;
import Util.Decl.FuncDecl;
import Util.Error.SemanticError;
import Util.Position;
import Util.Type.ReturnType;
import Util.Type.Type;

import java.util.*;

public class GlobalScope extends Scope {
    public HashMap<String, ClassDecl> classes = null;
    public HashMap<String, FuncDecl> functions = null;

    public GlobalScope() {
        super(null);
        classes = new HashMap<>();
        functions = new HashMap<>();
        functions.put("print", new FuncDecl(new ReturnType("void", 0), new ArrayList<>(List.of(new Type("string", 0)))));
        functions.put("println", new FuncDecl(new ReturnType("void", 0), new ArrayList<>(List.of(new Type("string", 0)))));
        functions.put("printInt", new FuncDecl(new ReturnType("void", 0), new ArrayList<>(List.of(new Type("int", 0)))));
        functions.put("printlnInt", new FuncDecl(new ReturnType("void", 0), new ArrayList<>(List.of(new Type("int", 0)))));
        functions.put("getString", new FuncDecl(new ReturnType("string", 0), new ArrayList<>()));
        functions.put("getInt", new FuncDecl(new ReturnType("int", 0), new ArrayList<>()));
        functions.put("toString", new FuncDecl(new ReturnType("string", 0), new ArrayList<>(List.of(new Type("int", 0)))));
        ClassDecl stringClass = new ClassDecl();
        stringClass.methods.put("length", new FuncDecl(new ReturnType("int", 0), new ArrayList<>()));
        stringClass.methods.put("substring", new FuncDecl(new ReturnType("string", 0), new ArrayList<>(List.of(new Type("int", 0), new Type("int", 0)))));
        stringClass.methods.put("parseInt", new FuncDecl(new ReturnType("int", 0), new ArrayList<>()));
        stringClass.methods.put("ord", new FuncDecl(new ReturnType("int", 0), new ArrayList<>(List.of(new Type("int", 0)))));
    }

    public void defineClass(String className, Position pos) {
        if (classes.containsKey(className))
            throw new SemanticError("[Multiple Definitions] class redefine: " + className, pos);
        if (functions.containsKey(className))
            throw new SemanticError("[Multiple Definitions] class and function name duplicate: " + className, pos);
        classes.put(className, new ClassDecl());
    }

    public void defineClassMember(String className, String memberName, Type type, Position pos) {
        if (classes.get(className).members.containsKey(memberName))
            throw new SemanticError("[Multiple Definitions] class member redefine: " + className + "." + memberName, pos);
        if (classes.get(className).methods.containsKey(memberName))
            throw new SemanticError("[Multiple Definitions] class method and member name duplicate: " + className + "." + memberName, pos);
        classes.get(className).members.put(memberName, type);
    }

    public void defineClassMethod(String className, String funcName, ArrayList<Type> paramTypes, ReturnType type, Position pos) {
        if (Objects.equals(className, funcName))
            throw new SemanticError("[Invalid Builder] class builder should not have return type or parameters: " + className, pos);
        if (classes.get(className).methods.containsKey(funcName))
            throw new SemanticError("[Multiple Definitions] class function redefine: " + className + "." + funcName, pos);
        if (classes.get(className).members.containsKey(funcName))
            throw new SemanticError("[Multiple Definitions] class member and method name duplicate: " + className + "." + funcName, pos);
        FuncDecl func = new FuncDecl(type, paramTypes);
        classes.get(className).methods.put(funcName, func);
    }

    public void defineFunc(String funcName, ArrayList<Type> paramTypes, ReturnType type, Position pos) {
        FuncDecl func = new FuncDecl(type, paramTypes);
        if (classes.containsKey(funcName))
            throw new SemanticError("[Multiple Definitions] class and function name duplicate: " + funcName, pos);
        if (functions.containsKey(funcName))
            throw new SemanticError("[Multiple Definitions] function redefine: " + funcName, pos);
        if (vars.containsKey(funcName))
            throw new SemanticError("[Multiple Definitions] function and variable name duplicate: " + funcName, pos);
        functions.put(funcName, func);
    }

    @Override
    public void defineVar(String name, Type type, Position pos) {
        if (functions.containsKey(name))
            throw new SemanticError("[Multiple Definitions] variable and function name duplicate: " + name, pos);
        super.defineVar(name, type, pos);
    }

    public Type getClassMember(String className, String memberName) {
        if (classes.get(className).members.containsKey(memberName))
            return classes.get(className).members.get(memberName);
        return null;
    }

    public FuncDecl getClassMethod(String className, String funcName) {
        if (classes.get(className).methods.containsKey(funcName))
            return classes.get(className).methods.get(funcName);
        return null;
    }

    public FuncDecl getFunc(String name) {
        if (functions.containsKey(name))
            return functions.get(name);
        return null;
    }

    public boolean isClassDefined(String name) {
        return classes.containsKey(name);
    }
}
