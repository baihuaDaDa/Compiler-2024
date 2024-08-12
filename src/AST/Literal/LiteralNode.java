package AST.Literal;

import AST.ASTNode;
import Util.Position;
import AST.ASTVisitor;
import Util.Type.ExprType;

public class LiteralNode extends ASTNode {
    public ExprType type = null;
    public int constInt = 0;
    public String constString = null;
    public boolean constLogic = false;
    public ConstArrayNode constArray = null;

    public LiteralNode(Position pos) {
        super(pos);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
