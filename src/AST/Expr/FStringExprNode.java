package AST.Expr;

import AST.ASTNode;
import AST.ASTVisitor;
import Util.Position;
import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;

public class FStringExprNode extends ASTNode {
    public String front = null, back = null;
    public ArrayList<Pair<String, ExprNode>> exprList = null;
    public boolean isExpr = false;

    public FStringExprNode(Position pos) {
        super(pos);
        exprList = new ArrayList<>();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
