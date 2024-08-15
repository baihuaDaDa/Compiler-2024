package Util.Decl;

import AST.Definition.FuncDefNode;
import Util.Type.ReturnType;
import Util.Type.Type;

import java.util.ArrayList;

public class FuncDecl {
    public ReturnType type = null;
    public ArrayList<Type> paramTypes = null;

    public FuncDecl(ReturnType type, ArrayList<Type> paramTypes) {
        this.type = type;
        this.paramTypes = paramTypes;
    }

    public FuncDecl(FuncDefNode node) {
        type = node.type;
        paramTypes = new ArrayList<>();
        for (var param : node.paramList)
            paramTypes.add(param.a);
    }
}
