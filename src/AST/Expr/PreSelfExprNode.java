package AST.Expr;

import AST.ASTVisitor;
import Util.Position;

public class PreSelfExprNode extends ExprNode {
    public boolean isIncrement;
    public ExprNode expr = null;

    public PreSelfExprNode(Position pos) {
        super(pos);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
