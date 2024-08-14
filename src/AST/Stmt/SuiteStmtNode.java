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

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
