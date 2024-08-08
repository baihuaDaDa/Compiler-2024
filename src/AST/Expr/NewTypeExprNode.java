package AST.Expr;

import AST.ASTVisitor;
import Util.Type.BaseType;
import Util.Position;

public class NewTypeExprNode extends ExprNode {
    public BaseType type = null;

    public NewTypeExprNode(Position pos, BaseType type) {
        super(pos);
        this.type = type;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
