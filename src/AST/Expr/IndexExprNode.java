package AST.Expr;

import AST.ASTVisitor;
import Util.Position;

public class IndexExprNode extends ExprNode {
    public ExprNode array = null, index = null;

    public IndexExprNode(Position pos) {
        super(pos);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
