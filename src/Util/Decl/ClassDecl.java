package Util.Decl;

import AST.ClassDef.ClassDefNode;
import Util.Type.Type;

import javax.swing.text.html.parser.Parser;
import java.util.HashMap;

public class ClassDecl {
    public int builderCnt = 0;
    public HashMap<String, Type> members;
    public HashMap<String, FuncDecl> methods;

    public ClassDecl() {
        members = new HashMap<>();
        methods = new HashMap<>();
    }

    public ClassDecl(ClassDefNode classDef) {
        members = new HashMap<>();
        methods = new HashMap<>();
        builderCnt = classDef.classBuilder.size();
        for (var varDef : classDef.varDefList)
            for (var var : varDef.vars)
                members.put(var.a, varDef.type);
        for (var funcDef : classDef.funcDefList)
            methods.put(funcDef.funcName, new FuncDecl(funcDef));
    }
}
