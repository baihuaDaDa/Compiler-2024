package AST.Expr;

import AST.ASTVisitor;
import Util.Position;

public class UnaryExprNode extends ExprNode {
    private String op = null;
    private ExprNode expr = null;

    public UnaryExprNode(Position pos, String op, ExprNode expr) {
        super(pos);
        this.op = op;
        this.expr = expr;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
