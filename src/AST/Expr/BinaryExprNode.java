package AST.Expr;

import AST.ASTVisitor;
import Util.Position;

public class BinaryExprNode extends ExprNode {
    private String op = null;
    public ExprNode lhs = null, rhs = null;

    public BinaryExprNode(Position pos, String op, ExprNode lhs, ExprNode rhs) {
        super(pos);
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
