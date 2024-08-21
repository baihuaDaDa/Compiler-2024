package Midend;

import IR.IRBlock;
import IR.IRProgram;
import Util.Scope.GlobalScope;
import Util.Scope.IRScope;

public class IRBuilder {
    public GlobalScope gScope;
    public IRProgram program;
    public IRBlock curBlock;
    public IRScope curScope;
}
