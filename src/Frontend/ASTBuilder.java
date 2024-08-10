package Frontend;

import AST.ASTNode;
import Parser.MxBaseVisitor;
import Parser.MxParser;

public class ASTBuilder extends MxBaseVisitor<ASTNode> {
    @Override
    public ASTNode visitProgram(MxParser.ProgramContext ctx) {
    }

    @Override
    public ASTNode visitFuncDef(MxParser.FuncDefContext ctx) {
    }

    @Override
    public ASTNode visitClassDef(MxParser.ClassDefContext ctx) {
    }

    @Override
    public ASTNode visitClassBuild(MxParser.ClassBuildContext ctx) {
    }

    @Override
    public ASTNode visitSuite(MxParser.SuiteContext ctx) {
    }

    @Override
    public ASTNode visitVarDefStmt(MxParser.VarDefStmtContext ctx) {
    }

    @Override
    public ASTNode visitExprStmt(MxParser.ExprStmtContext ctx) {
    }

    @Override
    public ASTNode visitIfStmt(MxParser.IfStmtContext ctx) {
    }

    @Override
    public ASTNode visitForStmt(MxParser.ForStmtContext ctx) {
    }

    @Override
    public ASTNode visitWhileStmt(MxParser.WhileStmtContext ctx) {
    }

    @Override
    public ASTNode visitBreakStmt(MxParser.BreakStmtContext ctx) {
    }

    @Override
    public ASTNode visitContinueStmt(MxParser.ContinueStmtContext ctx) {
    }

    @Override
    public ASTNode visitReturnStmt(MxParser.ReturnStmtContext ctx) {
    }

    @Override
    public ASTNode visitEmptyStmt(MxParser.EmptyStmtContext ctx) {
    }

    @Override
    public ASTNode visitSuiteStmt(MxParser.SuiteStmtContext ctx) {
    }

    @Override
    public ASTNode visitVarDef(MxParser.VarDefContext ctx) {
    }

    @Override
    public ASTNode visitNewArrayExpr(MxParser.NewArrayExprContext ctx) {
    }

    @Override
    public ASTNode visitNewEmptyArrayExpr(MxParser.NewEmptyArrayExprContext ctx) {
    }

    @Override
    public ASTNode visitNewTypeExpr(MxParser.NewTypeExprContext ctx) {
    }

    @Override
    public ASTNode visitMemberExpr(MxParser.MemberExprContext ctx) {
    }

    @Override
    public ASTNode visitFuncCallExpr(MxParser.FuncCallExprContext ctx) {
    }

    @Override
    public ASTNode visitIndexExpr(MxParser.IndexExprContext ctx) {
    }

    @Override
    public ASTNode visitSucSelfExpr(MxParser.SucSelfExprContext ctx) {
    }

    @Override
    public ASTNode visitUnaryExpr(MxParser.UnaryExprContext ctx) {
    }

    @Override
    public ASTNode visitPreSelfExpr(MxParser.PreSelfExprContext ctx) {
    }

    @Override
    public ASTNode visitBinaryExpr(MxParser.BinaryExprContext ctx) {
    }

    @Override
    public ASTNode visitTernaryExpr(MxParser.TernaryExprContext ctx) {
    }

    @Override
    public ASTNode visitAssignExpr(MxParser.AssignExprContext ctx) {
    }

    @Override
    public ASTNode visitParenExpr(MxParser.ParenExprContext ctx) {
    }

    @Override
    public ASTNode visitAtomExpr(MxParser.AtomExprContext ctx) {
    }

    @Override
    public ASTNode visitFStringExpr(MxParser.FStringExprContext ctx) {
    }

    @Override
    public ASTNode visitType(MxParser.TypeContext ctx) {
    }

    @Override
    public ASTNode visitBaseType(MxParser.BaseTypeContext ctx) {
    }

    @Override
    public ASTNode visitDefaultType(MxParser.DefaultTypeContext ctx) {
    }

    @Override
    public ASTNode visitLiteral(MxParser.LiteralContext ctx) {
    }

    @Override
    public ASTNode visitLogic(MxParser.LogicContext ctx) {
    }

    @Override
    public ASTNode visitConstArray(MxParser.ConstArrayContext ctx) {
    }

}