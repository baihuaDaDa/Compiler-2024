package AST.Literal;

import AST.ASTNode;
import Util.Position;
import AST.ASTVisitor;

public class LiteralNode extends ASTNode {
    public boolean isInt = false, isString = false, isLogic = false, isArray = false, isNull = false;
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
