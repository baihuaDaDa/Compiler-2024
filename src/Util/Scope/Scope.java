package Util.Scope;

import Util.Error.SemanticError;
import Util.Position;
import Util.Type.BaseType;
import Util.Type.ExprType;
import Util.Type.ReturnType;
import Util.Type.Type;

import java.util.HashMap;

public class Scope {
    protected HashMap<String, Type> vars = null;
    protected Scope parent = null;
    public boolean isInClass = false;
    public boolean isInGlobalClass = false; // 是否是类的全局作用域（不可被子节点继承）
    public boolean isInFunc = false;
    public boolean isInLoop = false;
    public BaseType classType = null;
    public ReturnType returnType = null;
    public boolean isReturned = false; // 需要上传给父节点
    public Position loopPos = null; // 循环语句位置

    public Scope(Scope parent) {
        vars = new HashMap<>();
        if (parent == null)
            return;
        this.parent = parent;
        this.vars = new HashMap<>();
        isInClass = parent.isInClass;
        isInFunc = parent.isInFunc;
        isInLoop = parent.isInLoop;
        classType = parent.classType;
        returnType = parent.returnType;
        isReturned = parent.isReturned;
        loopPos = parent.loopPos;
    }

    public Scope(Scope parent, BaseType type) {
        this(parent);
        if (type instanceof ReturnType) {
            isInFunc = true;
            returnType = (ReturnType) type;
            if (returnType.isSameType(new ReturnType("void", 0)))
                isReturned = true;
        } else {
            isInClass = true;
            isInGlobalClass = true;
            classType = type;
        }
    }

    public Scope(Scope parent, Position loopPos) {
        this(parent);
        isInLoop = true;
        this.loopPos = loopPos;
    }

    public void defineVar(String name, Type type, Position pos) {
        if (vars.containsKey(name))
            throw new SemanticError("[Multiple Definitions] variable redefine: " + name, pos);
        vars.put(name, type);
    }

    public ExprType getIdentifier(String name) {
        if (vars.containsKey(name)) return new ExprType(vars.get(name));
        else if (parent != null) {
            if (parent instanceof GlobalScope) {
                if (isInClass) {
                    Type classMemberType = ((GlobalScope) parent).getClassMember(new Type(classType), name);
                    if (classMemberType != null)
                        return new ExprType(classMemberType);
                    ExprType classMethodType = ((GlobalScope) parent).getClassMethod(new Type(classType), name);
                    if (classMethodType != null)
                        return classMethodType;
                }
                ExprType funcType = ((GlobalScope) parent).getFunc(name);
                if (funcType != null)
                    return funcType;
            }
            return parent.getIdentifier(name);
        }
        else return null;
    }

    public Scope getParent() {
        parent.isReturned |= isReturned;
        return parent;
    }
}