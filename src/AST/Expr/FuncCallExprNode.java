package AST.Expr;

import AST.ASTVisitor;
import Util.Position;

import java.util.ArrayList;

public class FuncCallExprNode extends ExprNode {
    public ExprNode func = null;
    public ArrayList<ExprNode> args = null;

    public FuncCallExprNode(Position pos) {
        super(pos);
        args = new ArrayList<>();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
