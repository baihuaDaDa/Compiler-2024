package AST.Expr;

import AST.ASTVisitor;
import Util.Position;

public class MemberExprNode extends ExprNode {
    public ExprNode classExpr = null;
    public String memberName = null;

    public MemberExprNode(Position pos, ExprNode classExpr, String memberName) {
        super(pos);
        this.classExpr = classExpr;
        this.memberName = memberName;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
