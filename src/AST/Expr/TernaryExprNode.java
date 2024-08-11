package AST.Expr;

import AST.ASTVisitor;
import Util.Position;

public class TernaryExprNode extends ExprNode {
    public ExprNode condition = null, thenExpr = null, elseExpr = null;

    public TernaryExprNode(Position pos) {
        super(pos);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
