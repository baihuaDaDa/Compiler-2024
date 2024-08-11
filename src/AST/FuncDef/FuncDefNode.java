package AST.FuncDef;

import AST.ASTNode;
import AST.ASTVisitor;
import AST.Stmt.SuiteStmtNode;
import Util.Type.ReturnType;
import Util.Type.Type;
import Util.Position;
import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;

public class FuncDefNode extends ASTNode {
    public ReturnType type;
    public String funcName;
    public ArrayList<Pair<Type, String>> paramList;
    public SuiteStmtNode body;

    public FuncDefNode(Position pos) {
        super(pos);
        paramList = new ArrayList<>();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}