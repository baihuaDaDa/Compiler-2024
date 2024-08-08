package AST.Expr;

import AST.ASTVisitor;
import AST.Literal.ConstArrayNode;
import Util.Position;
import Util.Type.Type;

public class NewArrayExprNode extends ExprNode {
    public Type type = null;
    public ConstArrayNode constArray = null;

    public NewArrayExprNode(Position pos, Type type, ConstArrayNode constArray) {
        super(pos);
        this.type = type;
        this.constArray = constArray;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
