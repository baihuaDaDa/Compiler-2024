package AST;

import AST.Program.*;
import AST.ClassBuild.*;
import AST.ClassDef.*;
import AST.FuncDef.*;
import AST.Stmt.*;
import AST.Expr.*;
import AST.VarDef.*;
import AST.Literal.*;

public interface ASTVisitor {
    void visit(ProgramNode node);

    void visit(ClassBuildNode node);

    void visit(ClassDefNode node);

    void visit(FuncDefNode node);

    void visit(StmtNode node);
    void visit(VarDefStmtNode node);
    void visit(IfStmtNode node);
    void visit(ForStmtNode node);
    void visit(WhileStmtNode node);
    void visit(ReturnStmtNode node);
    void visit(BreakStmtNode node);
    void visit(ContinueStmtNode node);
    void visit(SuiteStmtNode node);

    void visit(ExprNode node);
    void visit(NewTypeExprNode node);
    void visit(FuncCallExprNode node);
    void visit(MemberExprNode node);
    void visit(IndexExprNode node);
    void visit(UnaryExprNode node);
    void visit(BinaryExprNode node);
    void visit(AssignExprNode node);
    void visit(AtomExprNode node);

    void visit(VarDefNode node);

    void visit(FStringExprNode node);

    void visit(LiteralNode node);
    void visit(ConstArrayNode node);
}