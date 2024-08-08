package AST.Literal;

import AST.ASTNode;
import AST.ASTVisitor;
import Util.Position;

import java.util.ArrayList;

public class ConstArrayNode extends ASTNode {
    public ArrayList<LiteralNode> constArray = null;

    public ConstArrayNode(Position pos) {
        super(pos);
        constArray = new ArrayList<>();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
