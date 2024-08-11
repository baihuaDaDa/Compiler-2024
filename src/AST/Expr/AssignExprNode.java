package AST.Expr;

import AST.ASTVisitor;
import Util.Position;

public class AssignExprNode extends ExprNode {
    public ExprNode lhs = null, rhs = null;

    public AssignExprNode(Position pos) {
        super(pos);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
