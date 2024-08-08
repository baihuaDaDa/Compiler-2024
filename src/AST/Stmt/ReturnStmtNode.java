package AST.Stmt;

import AST.ASTVisitor;
import AST.Expr.ExprNode;
import Util.Position;

public class ReturnStmtNode extends StmtNode {
    public ExprNode returnValue = null;
    public boolean isVoid = false;

    ReturnStmtNode(Position pos, boolean isVoid, ExprNode returnValue) {
        super(pos);
        if (isVoid) {
            this.isVoid = true;
            this.returnValue = returnValue;
        }
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
