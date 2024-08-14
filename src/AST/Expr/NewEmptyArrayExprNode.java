package AST.Expr;

import AST.ASTVisitor;
import Util.Position;
import Util.Type.Type;

import java.util.ArrayList;

public class NewEmptyArrayExprNode extends ExprNode {
    public Type arrayType = null;
    public ArrayList<ExprNode> sizeList = null;

    public NewEmptyArrayExprNode(Position pos) {
        super(pos);
        sizeList = new ArrayList<>();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
