package Util.IRObject;

import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRVariable;

public class IRExpr {
    public IREntity value;
    public IRVariable ptr;
    public IRFunc func;

    public IRExpr(IREntity value) { // 右值
        this.value = value;
    }

    public IRExpr(IREntity value, IRVariable ptr) { // 左值
        this.value = value;
        this.ptr = ptr;
    }

    public IRExpr(IRFunc func) { // 函数签名
        this.func = func;
    }
}
