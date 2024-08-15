package AST.Program;

import AST.ASTNode;
import AST.ASTVisitor;
import AST.Definition.DefinitionNode;
import Util.Position;

import java.util.ArrayList;

public class ProgramNode extends ASTNode {
    public ArrayList<DefinitionNode> defs = null;

    public ProgramNode(Position pos) {
        super(pos);
        defs = new ArrayList<>();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
