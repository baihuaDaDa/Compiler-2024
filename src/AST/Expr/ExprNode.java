package AST.Expr;

import AST.ASTNode;
import AST.ASTVisitor;
import Util.Type.ExprType;
import Util.Position;

abstract public class ExprNode extends ASTNode {
    public ExprType type = null;
    public boolean isLeftValue = false;

    public ExprNode(Position pos) {
        super(pos);
    }

    @Override
    abstract public void accept(ASTVisitor visitor);
}
