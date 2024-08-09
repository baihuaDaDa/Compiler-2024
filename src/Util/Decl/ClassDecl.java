package Util.Decl;

import Util.Type.Type;

import java.util.HashMap;

public class ClassDecl {
    public HashMap<String, Type> members = null;
    public HashMap<String, FuncDecl> methods = null;

    public ClassDecl() {
        members = new HashMap<>();
        methods = new HashMap<>();
    }
}
