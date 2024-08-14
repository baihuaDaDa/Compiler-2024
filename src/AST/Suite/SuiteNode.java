package AST.Suite;

import AST.ASTNode;
import AST.ASTVisitor;
import AST.Stmt.StmtNode;
import Util.Position;

import java.util.ArrayList;

public class SuiteNode extends ASTNode {
    public ArrayList<StmtNode> stmts = null;

    public SuiteNode(Position pos) {
        super(pos);
        stmts = new ArrayList<>();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
