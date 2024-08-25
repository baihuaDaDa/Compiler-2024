package IR;

import IR.Module.*;
import Util.IRObject.IREntity.IRGlobalPtr;
import Util.Type.IRType;

import java.util.ArrayList;

public class IRProgram {
    public ArrayList<FuncDeclMod> funcDecls = null;
    public ArrayList<StructDefMod> structDefs = null;
    public ArrayList<StringLiteralDefMod> stringLiteralDefs = null;
    public ArrayList<GlobalVarDefMod> globalVarDefs = null;
    public ArrayList<FuncDefMod> funcDefs = null;
    public FuncDefMod initFunc = null;
    public FuncDefMod mainFunc = null;
    public int constArrayCnt = 0;

    public IRProgram() {
        funcDecls = new ArrayList<>();
        structDefs = new ArrayList<>();
        funcDefs = new ArrayList<>();
        globalVarDefs = new ArrayList<>();
        stringLiteralDefs = new ArrayList<>();
        funcDecls.add(new FuncDeclMod(new IRType("void"), "print", "ptr"));
        funcDecls.add(new FuncDeclMod(new IRType("void"), "println", "ptr"));
        funcDecls.add(new FuncDeclMod(new IRType("void"), "printInt", "i32"));
        funcDecls.add(new FuncDeclMod(new IRType("void"), "printlnInt", "i32"));
        funcDecls.add(new FuncDeclMod(new IRType("ptr"), "getString"));
        funcDecls.add(new FuncDeclMod(new IRType("i32"), "getInt"));
        funcDecls.add(new FuncDeclMod(new IRType("ptr"), "toString", "i32"));
        funcDecls.add(new FuncDeclMod(new IRType("i32"), "string.length", "ptr"));
        funcDecls.add(new FuncDeclMod(new IRType("ptr"), "string.substring", "ptr", "i32", "i32"));
        funcDecls.add(new FuncDeclMod(new IRType("i32"), "string.parseInt", "ptr"));
        funcDecls.add(new FuncDeclMod(new IRType("i32"), "string.ord", "ptr", "i32"));
        funcDecls.add(new FuncDeclMod(new IRType("ptr"), "string.add", "ptr", "ptr"));
        funcDecls.add(new FuncDeclMod(new IRType("i1"), "string.equal", "ptr", "ptr"));
        funcDecls.add(new FuncDeclMod(new IRType("i1"), "string.notEqual", "ptr", "ptr"));
        funcDecls.add(new FuncDeclMod(new IRType("i1"), "string.less", "ptr", "ptr"));
        funcDecls.add(new FuncDeclMod(new IRType("i1"), "string.lessOrEqual", "ptr", "ptr"));
        funcDecls.add(new FuncDeclMod(new IRType("i1"), "string.greater", "ptr", "ptr"));
        funcDecls.add(new FuncDeclMod(new IRType("i1"), "string.greaterOrEqual", "ptr", "ptr"));
        funcDecls.add(new FuncDeclMod(new IRType("i32"), "array.size", "ptr"));
        funcDecls.add(new FuncDeclMod(new IRType("ptr"), ".builtin.malloc", "i32"));
        funcDecls.add(new FuncDeclMod(new IRType("ptr"), ".builtin.calloc", "i32", "i32"));
        funcDecls.add(new FuncDeclMod(new IRType("ptr"), "array.malloc", "i32", "i32"));
        funcDecls.add(new FuncDeclMod(new IRType("ptr"), "array.calloc", "i32", "i32"));
        funcDecls.add(new FuncDeclMod(new IRType("ptr"), "array.copy", "ptr", "i32", "i32"));
        funcDecls.add(new FuncDeclMod(new IRType("ptr"), ".builtin.boolToString", "i1"));
    }

    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (var funcDecl : funcDecls)
            ret.append(funcDecl).append("\n");
        for (var structDef : structDefs)
            ret.append(structDef).append("\n");
        for (var stringLiteralDef : stringLiteralDefs)
            ret.append(stringLiteralDef).append("\n");
        for (var globalVarDef : globalVarDefs)
            ret.append(globalVarDef).append("\n");
        for (var funcDef : funcDefs)
            ret.append(funcDef).append("\n");
        if (initFunc != null)
            ret.append(initFunc).append("\n");
        ret.append(mainFunc).append("\n");
        return ret.toString();
    }

    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
