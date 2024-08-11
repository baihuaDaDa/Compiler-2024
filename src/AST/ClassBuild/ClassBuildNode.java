package AST.ClassBuild;

import AST.ASTNode;
import AST.ASTVisitor;
import AST.Stmt.SuiteStmtNode;
import Util.Position;

public class ClassBuildNode extends ASTNode {
    public String className = null;
    public SuiteStmtNode body = null;

    public ClassBuildNode(Position pos) {
        super(pos);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
