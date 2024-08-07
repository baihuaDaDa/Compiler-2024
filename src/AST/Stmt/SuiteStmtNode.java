package AST.Stmt;

import AST.ASTVisitor;
import Util.Position;

import java.util.ArrayList;

public class SuiteStmtNode extends StmtNode {
    ArrayList<StmtNode> stmts = null;

    public SuiteStmtNode(Position pos) {
        super(pos);
        stmts = new ArrayList<>();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
