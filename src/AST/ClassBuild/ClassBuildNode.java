package AST.ClassBuild;

import AST.ASTNode;
import AST.ASTVisitor;
import AST.Stmt.SuiteStmtNode;
import Util.Position;

public class ClassBuildNode extends ASTNode {
    public String className = null;
    public SuiteStmtNode body = null;

    public ClassBuildNode(Position pos, String className, SuiteStmtNode body) {
        super(pos);
        this.className = className;
        this.body = body;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
