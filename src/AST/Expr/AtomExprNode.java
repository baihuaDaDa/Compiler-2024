package AST.Expr;

import AST.ASTVisitor;
import AST.Literal.LiteralNode;
import Util.Position;

public class AtomExprNode extends ExprNode {
    public boolean isThis = false, isIdentifier = false, isLiteral = false;
    public LiteralNode literal = null;
    public String identifier = null;

    public AtomExprNode(Position pos) {
        super(pos);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
