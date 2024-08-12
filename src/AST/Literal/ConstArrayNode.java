package AST.Literal;

import AST.ASTNode;
import AST.ASTVisitor;
import Util.Position;
import Util.Type.ExprType;

import java.util.ArrayList;

public class ConstArrayNode extends ASTNode {
    public ExprType elemType = null;
    public ArrayList<LiteralNode> constArray;

    public ConstArrayNode(Position pos) {
        super(pos);
        constArray = new ArrayList<>();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
