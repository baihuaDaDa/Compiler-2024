package Util.Scope;

import Util.Decl.ClassDecl;
import Util.Decl.FuncDecl;
import Util.Error.SemanticError;
import Util.Position;
import Util.Type.ExprType;
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
        classes.put("string", stringClass);
    }

    public void defineClass(String className, ClassDecl classDecl, Position pos) {
        if (classes.containsKey(className))
            throw new SemanticError("[Multiple Definitions] class redefine: " + className, pos);
        if (functions.containsKey(className))
            throw new SemanticError("[Multiple Definitions] class and function name duplicate: " + className, pos);
        classes.put(className, new ClassDecl());
        for (String memberName : classDecl.members.keySet())
            defineClassMember(className, memberName, classDecl.members.get(memberName), pos);
        for (String methodName : classDecl.methods.keySet())
            defineClassMethod(className, methodName, classDecl.methods.get(methodName), pos);
    }

    public void defineClassMember(String className, String memberName, Type type, Position pos) {
        if (classes.get(className).members.containsKey(memberName))
            throw new SemanticError("[Multiple Definitions] class member redefine: " + className + "." + memberName, pos);
        if (classes.get(className).methods.containsKey(memberName))
            throw new SemanticError("[Multiple Definitions] class method and member name duplicate: " + className + "." + memberName, pos);
        classes.get(className).members.put(memberName, type);
    }

    public void defineClassMethod(String className, String funcName, FuncDecl method, Position pos) {
        if (Objects.equals(className, funcName))
            throw new SemanticError("[Invalid Builder] class builder should not have return type or parameters: " + className, pos);
        if (classes.get(className).methods.containsKey(funcName))
            throw new SemanticError("[Multiple Definitions] class function redefine: " + className + "." + funcName, pos);
        if (classes.get(className).members.containsKey(funcName))
            throw new SemanticError("[Multiple Definitions] class member and method name duplicate: " + className + "." + funcName, pos);
        classes.get(className).methods.put(funcName, method);
    }

    public void defineFunc(String funcName, FuncDecl func, Position pos) {
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

    public Type getClassMember(Type classType, String memberName) {
        if (!classType.isClass || classType.dim > 0)
            return null;
        if (classes.get(classType.baseTypename).members.containsKey(memberName))
            return classes.get(classType.baseTypename).members.get(memberName);
        return null;
    }

    public ExprType getClassMethod(Type classType, String funcName) {
        if (classType.dim > 0) {
            if (funcName.equals("size"))
                return new ExprType(funcName, new FuncDecl(new ReturnType("int", 0), new ArrayList<>()));
            else return null;
        }
        if (classes.get(classType.baseTypename).methods.containsKey(funcName))
            return new ExprType(funcName, classes.get(classType.baseTypename).methods.get(funcName));
        return null;
    }

    public ExprType getFunc(String name) {
        if (functions.containsKey(name))
            return new ExprType(name, functions.get(name));
        return null;
    }

    public boolean isClassDefined(String name) {
        return classes.containsKey(name);
    }
}
