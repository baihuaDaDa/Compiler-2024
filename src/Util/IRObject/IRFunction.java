package Util.IRObject;

import Util.IRObject.IREntity.IRVariable;
import Util.Type.IRType;

public class IRFunction {
    public String funcName;
    public IRVariable thisPtr;
    public IRType returnType;

    public IRFunction(String funcName, IRType returnType) {
        this.funcName = funcName;
        this.returnType = returnType;
    }

    public IRFunction(String funcName, IRType returnType, IRVariable thisPtr) { // for class methods
        this.funcName = funcName;
        this.returnType = returnType;
        this.thisPtr = thisPtr;
    }
}
