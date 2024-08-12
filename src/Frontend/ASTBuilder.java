package Frontend;

import AST.ASTNode;
import AST.ClassBuild.ClassBuildNode;
import AST.ClassDef.ClassDefNode;
import AST.FuncDef.FuncDefNode;
import AST.Program.ProgramNode;
import AST.Stmt.*;
import AST.Literal.*;
import AST.Expr.*;
import AST.VarDef.*;
import Parser.MxBaseVisitor;
import Parser.MxParser;
import Util.Error.SemanticError;
import Util.Position;
import Util.Type.BaseType;
import Util.Type.ReturnType;
import Util.Type.Type;
import org.antlr.v4.runtime.misc.Pair;

// TODO 转义字符的转换
// TODO 保留词

public class ASTBuilder extends MxBaseVisitor<ASTNode> {
    @Override
    public ASTNode visitProgram(MxParser.ProgramContext ctx) {
        ProgramNode program = new ProgramNode(new Position(ctx));
        for (var varDef : ctx.varDef())
            program.varDefs.add((VarDefNode) visit(varDef));
        for (var classDef : ctx.classDef())
            program.classDefs.add((ClassDefNode) visit(classDef));
        for (var funcDef : ctx.funcDef())
            program.funcDefs.add((FuncDefNode) visit(funcDef));
        return program;
    }

    @Override
    public ASTNode visitFuncDef(MxParser.FuncDefContext ctx) {
        FuncDefNode funcDef = new FuncDefNode(new Position(ctx));
        funcDef.type = new ReturnType(ctx.returnType());
        funcDef.funcName = ctx.Identifier(0).getText();
        var paramTypes = ctx.type();
        var paramNames = ctx.Identifier();
        for (int i = 0; i < paramTypes.size(); i++)
            funcDef.paramList.add(new Pair<>(new Type(paramTypes.get(i)), paramNames.get(i + 1).getText()));
        funcDef.body = (SuiteStmtNode) visit(ctx.suite());
        return funcDef;
    }

    @Override
    public ASTNode visitClassDef(MxParser.ClassDefContext ctx) {
        ClassDefNode classDef = new ClassDefNode(new Position(ctx));
        classDef.className = ctx.Identifier().getText();
        if (ctx.classBuild().size() > 1)
            throw new SemanticError("Multiple class constructors.", new Position(ctx));
        else if (!ctx.classBuild().isEmpty())
            classDef.classBuild = (ClassBuildNode) visit(ctx.classBuild(0));
        for (var funcDef : ctx.funcDef())
            classDef.funcDefList.add((FuncDefNode) visit(funcDef));
        for (var varDef : ctx.varDef())
            classDef.varDefList.add((VarDefNode) visit(varDef));
        return classDef;
    }

    @Override
    public ASTNode visitClassBuild(MxParser.ClassBuildContext ctx) {
        ClassBuildNode classBuild = new ClassBuildNode(new Position(ctx));
        classBuild.className = ctx.Identifier().getText();
        classBuild.body = (SuiteStmtNode) visit(ctx.suite());
        return classBuild;
    }

    @Override
    public ASTNode visitVarDefStmt(MxParser.VarDefStmtContext ctx) {
        VarDefStmtNode varDefStmt = new VarDefStmtNode(new Position(ctx));
        varDefStmt.varDef = (VarDefNode) visit(ctx.varDef());
        return varDefStmt;
    }

    @Override
    public ASTNode visitExprStmt(MxParser.ExprStmtContext ctx) {
        ExprStmtNode exprStmt = new ExprStmtNode(new Position(ctx));
        exprStmt.expr = (ExprNode) visit(ctx.expression());
        return exprStmt;
    }

    @Override
    public ASTNode visitIfStmt(MxParser.IfStmtContext ctx) {
        IfStmtNode ifStmt = new IfStmtNode(new Position(ctx));
        ifStmt.condition = (ExprNode) visit(ctx.condition);
        ifStmt.thenStmt = (StmtNode) visit(ctx.thenStmt);
        if (ctx.elseStmt != null)
            ifStmt.elseStmt = (StmtNode) visit(ctx.elseStmt);
        return ifStmt;
    }

    @Override
    public ASTNode visitForStmt(MxParser.ForStmtContext ctx) {
        ForStmtNode forStmt = new ForStmtNode(new Position(ctx));
        if (ctx.init != null)
            forStmt.init = (StmtNode) visit(ctx.init);
        if (ctx.cond != null)
            forStmt.cond = (ExprNode) visit(ctx.cond);
        if (ctx.step != null)
            forStmt.step = (ExprNode) visit(ctx.step);
        forStmt.body = (StmtNode) visit(ctx.body);
        return forStmt;
    }

    @Override
    public ASTNode visitWhileStmt(MxParser.WhileStmtContext ctx) {
        WhileStmtNode whileStmt = new WhileStmtNode(new Position(ctx));
        whileStmt.condition = (ExprNode) visit(ctx.expression());
        whileStmt.body = (StmtNode) visit(ctx.statement());
        return whileStmt;
    }

    @Override
    public ASTNode visitBreakStmt(MxParser.BreakStmtContext ctx) {
        return new BreakStmtNode(new Position(ctx));
    }

    @Override
    public ASTNode visitContinueStmt(MxParser.ContinueStmtContext ctx) {
        return new ContinueStmtNode(new Position(ctx));
    }

    @Override
    public ASTNode visitReturnStmt(MxParser.ReturnStmtContext ctx) {
        ReturnStmtNode returnStmt = new ReturnStmtNode(new Position(ctx));
        if (ctx.expression() != null)
            returnStmt.returnValue = (ExprNode) visit(ctx.expression());
        return returnStmt;
    }

    @Override
    public ASTNode visitSuiteStmt(MxParser.SuiteStmtContext ctx) {
        SuiteStmtNode suite = new SuiteStmtNode(new Position(ctx));
        for (var stmt : ctx.suite().statement()) {
            if (stmt instanceof MxParser.EmptyStmtContext) continue;
            suite.stmts.add((StmtNode) visit(stmt));
        }
        return suite;
    }

    @Override
    public ASTNode visitVarDef(MxParser.VarDefContext ctx) {
        VarDefNode varDef = new VarDefNode(new Position(ctx));
        varDef.type = new Type(ctx.type());
        for (var unit : ctx.varDefUnit())
            varDef.vars.add(new Pair<>(unit.Identifier().getText(), unit.expression() == null ? null : (ExprNode) visit(unit.expression())));
        return varDef;
    }

    @Override
    public ASTNode visitNewArrayExpr(MxParser.NewArrayExprContext ctx) {
        NewArrayExprNode newArrayExpr = new NewArrayExprNode(new Position(ctx));
        newArrayExpr.type = new Type(ctx.baseType().getText(), ctx.LeftBracket().size());
        newArrayExpr.constArray = (ConstArrayNode) visit(ctx.constArray());
        return newArrayExpr;
    }

    @Override
    public ASTNode visitNewEmptyArrayExpr(MxParser.NewEmptyArrayExprContext ctx) {
        NewEmptyArrayExprNode newEmptyArrayExpr = new NewEmptyArrayExprNode(new Position(ctx));
        newEmptyArrayExpr.type = new Type(ctx.baseType().getText(), ctx.LeftBracket().size());
        boolean noSize = false;
        for (int i = 0 ; i < ctx.children.size() ; i++) {
            if (ctx.children.get(i).getText().equals("[")) {
                if (noSize) {
                    if (ctx.children.get(++i) instanceof MxParser.ExpressionContext)
                        throw new SemanticError("New Array error.", new Position(ctx));
                } else {
                    if (ctx.children.get(++i) instanceof MxParser.ExpressionContext) {
                        newEmptyArrayExpr.sizeList.add((ExprNode) visit(ctx.children.get(i)));
                    } else noSize = true;
                }
            }
        }
        return newEmptyArrayExpr;
    }

    @Override
    public ASTNode visitNewTypeExpr(MxParser.NewTypeExprContext ctx) {
        NewTypeExprNode newTypeExpr = new NewTypeExprNode(new Position(ctx));
        newTypeExpr.type = new BaseType(ctx.baseType());
        return newTypeExpr;
    }

    @Override
    public ASTNode visitMemberExpr(MxParser.MemberExprContext ctx) {
        MemberExprNode memberExpr = new MemberExprNode(new Position(ctx));
        memberExpr.classExpr = (ExprNode) visit(ctx.expression());
        memberExpr.memberName = ctx.Identifier().getText();
        return memberExpr;
    }

    @Override
    public ASTNode visitFuncCallExpr(MxParser.FuncCallExprContext ctx) {
        FuncCallExprNode funcCallExpr = new FuncCallExprNode(new Position(ctx));
        funcCallExpr.func = (ExprNode) visit(ctx.expression(0));
        for (var arg : ctx.expression().subList(1, ctx.expression().size()))
            funcCallExpr.args.add((ExprNode) visit(arg));
        return funcCallExpr;
    }

    @Override
    public ASTNode visitIndexExpr(MxParser.IndexExprContext ctx) {
        IndexExprNode indexExpr = new IndexExprNode(new Position(ctx));
        indexExpr.array = (ExprNode) visit(ctx.expression(0));
        indexExpr.index = (ExprNode) visit(ctx.expression(1));
        return indexExpr;
    }

    @Override
    public ASTNode visitUnaryExpr(MxParser.UnaryExprContext ctx) {
        UnaryExprNode unaryExpr = new UnaryExprNode(new Position(ctx));
        unaryExpr.op = ctx.op.getText();
        unaryExpr.expr = (ExprNode) visit(ctx.expression());
        return unaryExpr;
    }

    @Override
    public ASTNode visitPreSelfExpr(MxParser.PreSelfExprContext ctx) {
        PreSelfExprNode preSelfExpr = new PreSelfExprNode(new Position(ctx));
        preSelfExpr.isIncrement = ctx.op.getText().equals("++");
        preSelfExpr.expr = (ExprNode) visit(ctx.expression());
        return preSelfExpr;
    }

    @Override
    public ASTNode visitBinaryExpr(MxParser.BinaryExprContext ctx) {
        BinaryExprNode binaryExpr = new BinaryExprNode(new Position(ctx));
        binaryExpr.lhs = (ExprNode) visit(ctx.expression(0));
        binaryExpr.rhs = (ExprNode) visit(ctx.expression(1));
        binaryExpr.op = ctx.op.getText();
        return binaryExpr;
    }

    @Override
    public ASTNode visitTernaryExpr(MxParser.TernaryExprContext ctx) {
        TernaryExprNode ternaryExpr = new TernaryExprNode(new Position(ctx));
        ternaryExpr.condition = (ExprNode) visit(ctx.condition);
        ternaryExpr.thenExpr = (ExprNode) visit(ctx.thenExpr);
        ternaryExpr.elseExpr = (ExprNode) visit(ctx.elseExpr);
        return ternaryExpr;
    }

    @Override
    public ASTNode visitAssignExpr(MxParser.AssignExprContext ctx) {
        AssignExprNode assignExpr = new AssignExprNode(new Position(ctx));
        assignExpr.lhs = (ExprNode) visit(ctx.expression(0));
        assignExpr.rhs = (ExprNode) visit(ctx.expression(1));
        return assignExpr;
    }

    @Override
    public ASTNode visitParenExpr(MxParser.ParenExprContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public ASTNode visitAtomExpr(MxParser.AtomExprContext ctx) {
        AtomExprNode atomExpr = new AtomExprNode(new Position(ctx));
        if (ctx.literal() != null) {
            atomExpr.isLiteral = true;
            atomExpr.literal = (LiteralNode) visit(ctx.literal());
        } else if (ctx.Identifier() != null) {
            atomExpr.isIdentifier = true;
            atomExpr.identifier = ctx.Identifier().getText();
        } else if (ctx.This() != null) {
            atomExpr.isThis = true;
        }
        return atomExpr;
    }

    @Override
    public ASTNode visitFStringExpr(MxParser.FStringExprContext ctx) {
        FStringExprNode fStringExpr = new FStringExprNode(new Position(ctx));
        var fString = ctx.fString();
        if (fString.Quote2Quote() != null) {
            fStringExpr.isExpr = false;
            fStringExpr.front = fString.Quote2Quote().getText();
            fStringExpr.front = fStringExpr.front.substring(2, fStringExpr.front.length() - 1);
        } else if (fString.Quote2Dollar() != null) {
            fStringExpr.isExpr = true;
            fStringExpr.front = fString.Quote2Dollar().getText();
            fStringExpr.front = fStringExpr.front.substring(2, fStringExpr.front.length() - 1);
            fStringExpr.back = fString.Dollar2Quote().getText();
            fStringExpr.back = fStringExpr.back.substring(1, fStringExpr.back.length() - 1);
            var expressions = fString.expression();
            var strings = fString.Dollar2Dollar();
            fStringExpr.exprList.add(new Pair<>(null, (ExprNode) visit(expressions.getFirst())));
            for (int i = 0; i < strings.size(); i++) {
                String middleString = strings.get(i).getText();
                fStringExpr.exprList.add(new Pair<>(middleString.substring(1, middleString.length() - 1), (ExprNode) visit(expressions.get(i + 1))));
            }
        }
        return fStringExpr;
    }

    @Override
    public ASTNode visitLiteral(MxParser.LiteralContext ctx) {
        LiteralNode literal = new LiteralNode(new Position(ctx));
        if (ctx.ConstDecInt() != null) {
            literal.constInt = Integer.parseInt(ctx.ConstDecInt().getText());
            literal.isInt = true;
        } else if (ctx.ConstString() != null) {
            literal.constString = ctx.ConstString().getText();
            literal.constString = literal.constString.substring(1, literal.constString.length() - 1);
            literal.isString = true;
        } else if (ctx.logic() != null) {
            literal.constLogic = ctx.logic().True() != null;
            literal.isLogic = true;
        } else if (ctx.constArray() != null) {
            literal.constArray = (ConstArrayNode) visit(ctx.constArray());
            literal.isArray = true;
        }
        return literal;
    }

    @Override
    public ASTNode visitConstArray(MxParser.ConstArrayContext ctx) {
        ConstArrayNode constArray = new ConstArrayNode(new Position(ctx));
        for (var literal : ctx.literal())
            constArray.constArray.add((LiteralNode) visit(literal));
        return constArray;
    }

}