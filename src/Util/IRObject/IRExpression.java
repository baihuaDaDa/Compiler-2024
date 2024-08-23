package Util.IRObject;

import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRVariable;

public class IRExpression {
    public IREntity value;
    public IRVariable ptr;
    public IRFunction func;

    public IRExpression(IREntity value) { // 右值
        this.value = value;
    }

    public IRExpression(IREntity value, IRVariable ptr) { // 左值
        this.value = value;
        this.ptr = ptr;
    }

    public IRExpression(IRFunction func) { // 函数签名
        this.func = func;
    }
}
