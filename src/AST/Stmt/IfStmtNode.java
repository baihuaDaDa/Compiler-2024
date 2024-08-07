package AST.Stmt;

import AST.ASTVisitor;
import AST.Expr.ExprNode;
import Util.Position;

public class IfStmtNode extends StmtNode {
    private ExprNode condition = null;
    private StmtNode thenStmt = null, elseStmt = null;

    public IfStmtNode(Position pos, ExprNode condition, StmtNode thenStmt, StmtNode elseStmt) {
        super(pos);
        this.condition = condition;
        this.thenStmt = thenStmt;
        this.elseStmt = elseStmt;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
