package AST.Stmt;

import AST.ASTVisitor;
import AST.Expr.ExprNode;
import Util.Position;

public class WhileStmtNode extends StmtNode {
    public ExprNode condition = null;
    public StmtNode body = null;

    public WhileStmtNode(Position pos, ExprNode condition, StmtNode body) {
        super(pos);
        this.condition = condition;
        this.body = body;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
