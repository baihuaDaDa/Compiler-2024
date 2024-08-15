package AST.Definition;

import AST.ASTNode;
import AST.ASTVisitor;
import AST.ClassBuild.ClassBuildNode;
import Util.Position;

import java.util.ArrayList;

public class ClassDefNode extends DefinitionNode {
    public String className = null;
    public ClassBuildNode classBuild = null;
    public ArrayList<VarDefNode> varDefList = null;
    public ArrayList<FuncDefNode> methodDefList = null;

    public ClassDefNode(Position pos) {
        super(pos);
        varDefList = new ArrayList<>();
        methodDefList = new ArrayList<>();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
