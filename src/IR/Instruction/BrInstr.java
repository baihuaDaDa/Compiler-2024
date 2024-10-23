package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRLiteral;
import Util.IRObject.IREntity.IRLocalVar;

import java.util.HashMap;
import java.util.HashSet;

public class BrInstr extends Instruction {
    public IRLocalVar cond;
    public IRBlock thenBlock, elseBlock;

    public BrInstr(IRBlock parent, IRLocalVar cond, IRBlock thenBlock, IRBlock elseBlock) {
        super(parent);
        this.cond = cond;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    @Override
    public String toString() {
        if (cond != null) return "br i1 " + cond + ", label %" + thenBlock.label + ", label %" + elseBlock.label;
        return "br label %" + thenBlock.label;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }


    @Override
    public void rename(HashMap<IRLocalVar, IREntity> renameMap) {
        if (cond != null && cond instanceof IRLocalVar localCond && renameMap.containsKey(localCond))  {
            IREntity newVar = renameMap.get(localCond);
            if (newVar instanceof IRLocalVar localVar) cond = localVar;
            else if (newVar instanceof IRLiteral literal) {
                // TODO 可能需要修改 CFG 和 DomTree
                cond = null;
                if (literal.value == 1) elseBlock = null;
                else {
                    thenBlock = elseBlock;
                    elseBlock = null;
                }
            }
            else throw new RuntimeException("Unknown entity in renameMap");
        }
    }

    @Override
    public IRLocalVar getDef() {
        return null;
    }

    @Override
    public HashSet<IRLocalVar> getUse() {
        return new HashSet<>() {{
            if (cond != null) add(cond);
        }};
    }
}
