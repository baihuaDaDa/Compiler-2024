package Util.Decl;

import AST.Definition.ClassDefNode;
import Util.Type.Type;

import java.util.HashMap;

public class ClassDecl {
    public HashMap<String, Type> members;
    public HashMap<String, FuncDecl> methods;

    public ClassDecl() {
        members = new HashMap<>();
        methods = new HashMap<>();
    }

    public ClassDecl(ClassDefNode classDef) {
        members = new HashMap<>();
        methods = new HashMap<>();
        for (var varDef : classDef.varDefList)
            for (var var : varDef.vars)
                members.put(var.a, varDef.type);
        for (var funcDef : classDef.methodDefList)
            methods.put(funcDef.funcName, new FuncDecl(funcDef));
    }
}
