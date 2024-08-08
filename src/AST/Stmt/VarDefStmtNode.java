package AST.Stmt;

import AST.ASTVisitor;
import AST.VarDef.VarDefNode;
import Util.Position;

public class VarDefStmtNode extends StmtNode {
    public VarDefNode varDef;

    public VarDefStmtNode(Position pos, VarDefNode varDef) {
        super(pos);
        this.varDef = varDef;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
