package AST.Expr;

import AST.ASTVisitor;
import AST.Literal.ConstArrayNode;
import Util.Position;
import Util.Type.Type;

public class NewArrayExprNode extends ExprNode {
    public Type arrayType = null;
    public ConstArrayNode constArray = null;

    public NewArrayExprNode(Position pos) {
        super(pos);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
