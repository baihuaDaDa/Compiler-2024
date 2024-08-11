package AST.Expr;

import AST.ASTVisitor;
import Util.Position;

public class BinaryExprNode extends ExprNode {
    public String op = null;
    public ExprNode lhs = null, rhs = null;

    public BinaryExprNode(Position pos) {
        super(pos);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
