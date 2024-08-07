package AST.Expr;

import AST.ASTVisitor;
import Util.Position;

public class IndexExprNode extends ExprNode {
    private ExprNode array = null, index = null;

    public IndexExprNode(Position pos, ExprNode array, ExprNode index) {
        super(pos);
        this.array = array;
        this.index = index;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
