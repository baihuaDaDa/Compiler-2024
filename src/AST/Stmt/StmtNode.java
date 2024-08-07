package AST.Stmt;

import AST.ASTVisitor;
import Util.Position;
import AST.ASTNode;

abstract public class StmtNode extends ASTNode {
    public StmtNode(Position pos) {
        super(pos);
    }

    @Override
    abstract public void accept(ASTVisitor visitor);
}
