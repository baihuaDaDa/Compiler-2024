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

    // 单语句块
    public SuiteNode(Position pos, StmtNode stmt) {
        super(pos);
        stmts = new ArrayList<>();
        stmts.add(stmt);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
