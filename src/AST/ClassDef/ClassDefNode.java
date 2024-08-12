package AST.ClassDef;

import AST.ASTNode;
import AST.ASTVisitor;
import AST.ClassBuild.ClassBuildNode;
import AST.FuncDef.FuncDefNode;
import AST.VarDef.VarDefNode;
import Util.Position;

import java.util.ArrayList;

public class ClassDefNode extends ASTNode {
    public String className = null;
    public ClassBuildNode classBuild = null;
    public ArrayList<VarDefNode> varDefList = null;
    public ArrayList<FuncDefNode> funcDefList = null;

    public ClassDefNode(Position pos) {
        super(pos);
        varDefList = new ArrayList<>();
        funcDefList = new ArrayList<>();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
