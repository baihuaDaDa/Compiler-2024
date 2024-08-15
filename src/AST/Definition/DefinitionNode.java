package AST.Definition;

import AST.ASTNode;
import AST.ASTVisitor;
import Util.Position;

abstract public class DefinitionNode extends ASTNode {
    public DefinitionNode(Position pos) {
        super(pos);
    }

    @Override
    abstract public void accept(ASTVisitor visitor);
}
