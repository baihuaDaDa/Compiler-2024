package AST.Expr;

import AST.ASTVisitor;
import Util.Type.BaseType;
import Util.Position;

public class NewTypeExprNode extends ExprNode {
    public BaseType newType = null;

    public NewTypeExprNode(Position pos) {
        super(pos);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
