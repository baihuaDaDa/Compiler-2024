package IR.Module;

import IR.IRVisitor;
import Util.IRObject.IREntity.IRGlobalPtr;
import Util.IRObject.IREntity.IRLiteral;

public class GlobalVarDefMod extends Module {
    public IRGlobalPtr globalVar;
    public IRLiteral init;

    public GlobalVarDefMod(IRGlobalPtr globalVar, IRLiteral init) {
        this.globalVar = globalVar;
        this.init = init;
    }

    @Override
    public String toString() {
        return globalVar + " = global " + init.type + " " + init + "\n";
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
