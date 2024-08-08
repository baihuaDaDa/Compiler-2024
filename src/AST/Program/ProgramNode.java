package AST.Program;

import AST.ASTNode;
import AST.ASTVisitor;
import AST.ClassDef.ClassDefNode;
import AST.FuncDef.FuncDefNode;
import AST.Stmt.VarDefStmtNode;
import Util.Position;

import java.util.ArrayList;

public class ProgramNode extends ASTNode {
    public ArrayList<FuncDefNode> funcDefs = null;
    public ArrayList<ClassDefNode> classDefs = null;
    public ArrayList<VarDefStmtNode> varDefs = null;

    public ProgramNode(Position pos) {
        super(pos);
        funcDefs = new ArrayList<>();
        classDefs = new ArrayList<>();
        varDefs = new ArrayList<>();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
