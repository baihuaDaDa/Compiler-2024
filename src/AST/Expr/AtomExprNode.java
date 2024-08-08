package AST.Expr;

import AST.ASTVisitor;
import AST.Literal.LiteralNode;
import Util.Position;

public class AtomExprNode extends ExprNode {
    public boolean isThis = false, isIdentifier = false, isLiteral = false;
    public LiteralNode literal = null;
    public String identifier = null;

    public AtomExprNode(Position pos, boolean isThis, boolean isIdentifier, boolean isLiteral, LiteralNode literal, String identifier) {
        super(pos);
        if (isThis) this.isThis = true;
        else if (isLiteral) {
            this.isLiteral = true;
            this.literal = literal;
        } else if (isIdentifier) {
            this.isIdentifier = true;
            this.identifier = identifier;
        }
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
