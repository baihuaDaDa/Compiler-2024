package AST.Expr;

import AST.ASTVisitor;
import AST.FString.FStringNode;
import Util.Position;

public class FStringExprNode extends ExprNode {
    public FStringNode fString = null;

    public FStringExprNode(Position pos, FStringNode fString) {
        super(pos);
        this.fString = fString;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
