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
}
