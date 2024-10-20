package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRLiteral;
import Util.IRObject.IREntity.IRLocalVar;

import java.util.HashMap;
import java.util.HashSet;

public class IcmpInstr extends Instruction {
    public String cond;
    public IRLocalVar result;
    public IREntity lhs, rhs;

    public IcmpInstr(IRBlock parent, String cond, IREntity lhs, IREntity rhs, IRLocalVar result) {
        super(parent);
        this.cond = cond;
        this.lhs = lhs;
        this.rhs = rhs;
        this.result = result;
    }

    @Override
    public String toString() {
        return result + " = icmp " + cond + " " + lhs.type + " " + lhs + ", " + rhs;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void rename(HashMap<IRLocalVar, IREntity> renameMap) {
        if (lhs instanceof IRLocalVar localLhs && renameMap.containsKey(localLhs)) lhs = renameMap.get(localLhs);
        if (rhs instanceof IRLocalVar localRhs && renameMap.containsKey(localRhs)) rhs = renameMap.get(localRhs);
    }

    @Override
    public IRLocalVar getDef() {
        return result;
    }

    @Override
    public HashSet<IRLocalVar> getUse() {
        return new HashSet<>() {{
            if (lhs instanceof IRLocalVar localLhs) add(localLhs);
            if (rhs instanceof IRLocalVar localRhs) add(localRhs);
        }};
    }

    static public String getCond(String op) {
        return switch (op) {
            case "==" -> "eq";
            case "!=" -> "ne";
            case "<" -> "slt";
            case "<=" -> "sle";
            case ">" -> "sgt";
            case ">=" -> "sge";
            default -> throw new RuntimeException("Unexpected operation: " + op);
        };
    }
}
