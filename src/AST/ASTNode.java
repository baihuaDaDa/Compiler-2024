package AST;

import Util.Position;

public abstract class ASTNode {
    public Position pos = null;

    public ASTNode(Position pos) {
        this.pos = pos;
    }

    public abstract void accept(ASTVisitor visitor);
}