package AST.Literal;

import AST.ASTNode;
import Util.Position;
import AST.ASTVisitor;

public class LiteralNode extends ASTNode {
    public boolean isInt = false, isString = false, isLogic = false, isArray = false, isNull = false;
    public int constInt = 0;
    public String constString = null;
    public boolean constLogic = false;
    public ConstArrayNode constArray = null;

    public LiteralNode(Position pos, boolean isInt, int constInt,
                       boolean isString, String constString,
                       boolean isLogic, boolean constLogic,
                       boolean isArray, ConstArrayNode constArray,
                       boolean isNull) {
        super(pos);
        if (isInt) {
            this.isInt = true;
            this.constInt = constInt;
        } else if (isString) {
            this.isString = true;
            this.constString = constString;
        } else if (isLogic) {
            this.isLogic = true;
            this.constLogic = constLogic;
        } else if (isArray) {
            this.isArray = true;
            this.constArray = constArray;
        } else if (isNull) {
            this.isNull = true;
        }
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
