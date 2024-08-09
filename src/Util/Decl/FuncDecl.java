package Util.Decl;

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
}
