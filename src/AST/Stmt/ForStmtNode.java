package AST.Stmt;

import AST.ASTVisitor;
import AST.Expr.ExprNode;
import Util.Position;

public class ForStmtNode extends StmtNode {
    public StmtNode init = null;
    public ExprNode cond = null, step = null;
    public SuiteStmtNode body = null;

    public ForStmtNode(Position pos) {
        super(pos);
    }

    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
