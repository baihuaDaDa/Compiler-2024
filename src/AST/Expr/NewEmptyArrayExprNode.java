package AST.Expr;

import AST.ASTVisitor;
import Util.Position;
import Util.Type.Type;

import java.util.ArrayList;

public class NewEmptyArrayExprNode extends ExprNode {
    public Type type = null;
    public ArrayList<ExprNode> sizeList = null;

    public NewEmptyArrayExprNode(Position pos) {
        super(pos);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
