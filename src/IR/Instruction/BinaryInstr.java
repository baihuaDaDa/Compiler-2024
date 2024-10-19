package IR.Instruction;

import IR.IRBlock;
import IR.IRVisitor;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRLocalVar;

import java.util.HashSet;

public class BinaryInstr extends Instruction {
    public String op;
    public IREntity lhs, rhs;
    public IRLocalVar result;

    public BinaryInstr(IRBlock parent, String op, IREntity lhs, IREntity rhs, IRLocalVar result) {
        super(parent);
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
        this.result = result;
    }

    @Override
    public String toString() {
        return result + " = " + op + " " + result.type + " " + lhs + ", " + rhs;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
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

    static public String getOp(String op) {
        return switch (op) {
            case "+" -> "add";
            case "-" -> "sub";
            case "*" -> "mul";
            case "/" -> "sdiv";
            case "%" -> "srem";
            case "<<" -> "shl";
            case ">>" -> "ashr";
            case "&", "&&" -> "and";
            case "|", "||" -> "or";
            case "^" -> "xor";
            default -> throw new RuntimeException("Unexpected operation: " + op);
        };
    }
}
