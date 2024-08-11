package Frontend;

import AST.ASTVisitor;
import AST.ClassBuild.ClassBuildNode;
import AST.ClassDef.ClassDefNode;
import AST.Expr.*;
import AST.Expr.FStringExprNode;
import AST.FuncDef.FuncDefNode;
import AST.Literal.ConstArrayNode;
import AST.Literal.LiteralNode;
import AST.Program.ProgramNode;
import AST.Stmt.*;
import AST.VarDef.VarDefNode;
import Util.Decl.ClassDecl;
import Util.Decl.FuncDecl;
import Util.Scope.GlobalScope;

public class SymbolCollector implements ASTVisitor {
    GlobalScope gScope;

    public SymbolCollector(GlobalScope gScope) {
        this.gScope = gScope;
    }

    public void visit(ProgramNode node) {
        for (ClassDefNode classDef : node.classDefs)
            classDef.accept(this);
        for (FuncDefNode funcDef : node.funcDefs)
            funcDef.accept(this);
    }

    public void visit(ClassBuildNode node) {}

    public void visit(ClassDefNode node) {
        gScope.defineClass(node.className, new ClassDecl(node), node.pos);
    }

    public void visit(FuncDefNode node) {
        gScope.defineFunc(node.funcName, new FuncDecl(node), node.pos);
    }

    public void visit(StmtNode node) {}
    public void visit(VarDefStmtNode node) {}
    public void visit(IfStmtNode node) {}
    public void visit(ForStmtNode node) {}
    public void visit(WhileStmtNode node) {}
    public void visit(ReturnStmtNode node) {}
    public void visit(BreakStmtNode node) {}
    public void visit(ContinueStmtNode node) {}
    public void visit(SuiteStmtNode node) {}

    public void visit(ExprNode node) {}
    public void visit(NewTypeExprNode node) {}
    public void visit(FuncCallExprNode node) {}
    public void visit(MemberExprNode node) {}
    public void visit(IndexExprNode node) {}
    public void visit(UnaryExprNode node) {}
    public void visit(BinaryExprNode node) {}
    public void visit(AssignExprNode node) {}
    public void visit(AtomExprNode node) {}

    public void visit(VarDefNode node) {}

    public void visit(FStringExprNode node) {}

    public void visit(LiteralNode node) {}
    public void visit(ConstArrayNode node) {}
}
