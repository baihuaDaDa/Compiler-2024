package Util.Decl;

import AST.Definition.ClassDefNode;
import Util.Type.Type;

import java.util.HashMap;

public class ClassDecl {
    public HashMap<String, Type> members;
    public HashMap<String, Integer> memberIndices;
    public HashMap<String, FuncDecl> methods;
    public boolean hasOverrideConstructor = false;
    public int size = 0; // IR中占用字节数

    public ClassDecl() {
        members = new HashMap<>();
        methods = new HashMap<>();
        memberIndices = new HashMap<>();
    }
}
