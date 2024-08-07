package AST.VarDef;

import AST.ASTNode;
import AST.ASTVisitor;
import AST.Expr.ExprNode;
import AST.Type.BaseType;
import Util.Position;
import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;

public class VarDefNode extends ASTNode {
    private ArrayList<Pair<String, ExprNode>> vars;

    public VarDefNode(Position pos) {
        super(pos);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
