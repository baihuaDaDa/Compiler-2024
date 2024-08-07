package AST.Expr;

import AST.ASTNode;
import AST.ASTVisitor;
import Util.Position;

abstract public class ExprNode extends ASTNode {
    private boolean isLeftValue = false;

    public ExprNode(Position pos) {
        super(pos);
    }

    @Override
    abstract public void accept(ASTVisitor visitor);
}
