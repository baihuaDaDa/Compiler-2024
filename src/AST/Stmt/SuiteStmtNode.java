package AST.Stmt;

import AST.ASTVisitor;
import AST.Suite.SuiteNode;
import Util.Position;

import java.util.ArrayList;

public class SuiteStmtNode extends StmtNode {
    public SuiteNode suite;

    public SuiteStmtNode(Position pos) {
        super(pos);
    }

    public SuiteStmtNode(Position pos, StmtNode stmt) {
        super(pos);
        suite = new SuiteNode(pos, stmt);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
