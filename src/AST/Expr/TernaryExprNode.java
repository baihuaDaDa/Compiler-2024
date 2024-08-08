package AST.Expr;

import AST.ASTVisitor;
import Util.Position;

public class TernaryExprNode extends ExprNode {
    public ExprNode condition = null, thenExpr = null, elseExpr = null;

    public TernaryExprNode(Position pos, ExprNode condition, ExprNode thenExpr, ExprNode elseExpr) {
        super(pos);
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
