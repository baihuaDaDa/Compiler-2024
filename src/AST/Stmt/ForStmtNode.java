package AST.Stmt;

import AST.ASTVisitor;
import AST.Expr.ExprNode;
import Util.Position;

public class ForStmtNode extends StmtNode {
    public ExprNode init = null, cond = null, step = null;
    public StmtNode body = null;

    public ForStmtNode(Position pos, ExprNode init, ExprNode cond, ExprNode step, StmtNode body) {
        super(pos);
    }

    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
