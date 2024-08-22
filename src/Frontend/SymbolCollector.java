package Frontend;

import AST.ASTVisitor;
import AST.ClassBuild.ClassBuildNode;
import AST.Definition.ClassDefNode;
import AST.Definition.DefinitionNode;
import AST.Expr.*;
import AST.Expr.FStringExprNode;
import AST.Definition.FuncDefNode;
import AST.Literal.ConstArrayNode;
import AST.Literal.LiteralNode;
import AST.Program.ProgramNode;
import AST.Stmt.*;
import AST.Suite.SuiteNode;
import AST.Definition.VarDefNode;
import Util.Decl.ClassDecl;
import Util.Decl.FuncDecl;
import Util.Scope.GlobalScope;

public class SymbolCollector implements ASTVisitor {
    GlobalScope gScope;

    public SymbolCollector(GlobalScope gScope) {
        this.gScope = gScope;
    }

    public void visit(ProgramNode node) {
        for (DefinitionNode def : node.defs)
            if (def instanceof ClassDefNode || def instanceof FuncDefNode)
                def.accept(this);
    }

    public void visit(ClassBuildNode node) {}

    public void visit(ClassDefNode node) {
        gScope.defineClass(node, node.classBuild != null);
    }

    public void visit(FuncDefNode node) {
        gScope.defineFunc(node.funcName, new FuncDecl(node), node.pos);
    }

    public void visit(SuiteNode node) {}

    public void visit(VarDefStmtNode node) {}
    public void visit(ExprStmtNode node) {}
    public void visit(IfStmtNode node) {}
    public void visit(ForStmtNode node) {}
    public void visit(WhileStmtNode node) {}
    public void visit(ReturnStmtNode node) {}
    public void visit(BreakStmtNode node) {}
    public void visit(ContinueStmtNode node) {}
    public void visit(EmptyStmtNode node) {}
    public void visit(SuiteStmtNode node) {}

    public void visit(NewArrayExprNode node) {}
    public void visit(NewEmptyArrayExprNode node) {}
    public void visit(NewTypeExprNode node) {}
    public void visit(FuncCallExprNode node) {}
    public void visit(MemberExprNode node) {}
    public void visit(IndexExprNode node) {}
    public void visit(PreSelfExprNode node) {}
    public void visit(UnaryExprNode node) {}
    public void visit(BinaryExprNode node) {}
    public void visit(TernaryExprNode node) {}
    public void visit(AssignExprNode node) {}
    public void visit(AtomExprNode node) {}
    public void visit(FStringExprNode node) {}

    public void visit(VarDefNode node) {}

    public void visit(LiteralNode node) {}
    public void visit(ConstArrayNode node) {}
}
