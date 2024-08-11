package AST.Stmt;

import AST.ASTVisitor;
import AST.Expr.ExprNode;
import Util.Position;

public class IfStmtNode extends StmtNode {
    public ExprNode condition = null;
    public StmtNode thenStmt = null, elseStmt = null;

    public IfStmtNode(Position pos) {
        super(pos);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
