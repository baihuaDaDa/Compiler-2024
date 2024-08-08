package AST.FString;

import AST.ASTNode;
import AST.ASTVisitor;
import AST.Expr.ExprNode;
import Util.Position;
import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;

public class FStringNode extends ASTNode {
    public String front = null, back = null;
    public ArrayList<Pair<String, ExprNode>> exprList = null;
    public boolean isExpr = false;

    public FStringNode(Position pos) {
        super(pos);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
