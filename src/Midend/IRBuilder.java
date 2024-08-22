package Midend;

import AST.ASTVisitor;
import AST.ClassBuild.ClassBuildNode;
import AST.Definition.ClassDefNode;
import AST.Definition.FuncDefNode;
import AST.Definition.VarDefNode;
import AST.Expr.*;
import AST.Literal.ConstArrayNode;
import AST.Literal.LiteralNode;
import AST.Program.ProgramNode;
import AST.Stmt.*;
import AST.Suite.SuiteNode;
import IR.*;
import IR.Instruction.*;
import IR.Module.FuncDefMod;
import IR.Module.StringLiteralDefMod;
import IR.Module.StructDefMod;
import Util.IRObject.IREntity.IREntity;
import Util.IRObject.IREntity.IRGlobalPtr;
import Util.IRObject.IREntity.IRLiteral;
import Util.IRObject.IREntity.IRLocalVar;
import Util.IRObject.IRExpr;
import Util.Scope.GlobalScope;
import Util.Scope.IRScope;
import Util.Type.IRType;

import java.util.ArrayList;

// TODO 没有return void或者没有constructor
// TODO new 类数组是否需要调用构造函数

public class IRBuilder implements ASTVisitor {
    public GlobalScope gScope;
    public IRProgram program;
    public IRBlock curBlock;
    public IRScope curScope;
    public IRExpr lastExpr;

    public IRBuilder(GlobalScope gScope) {
        this.gScope = gScope;
        this.program = new IRProgram();
    }

    public void visit(ProgramNode node) {
        for (var def : node.defs)
            def.accept(this);
        if (program.initFunc != null) {
            curBlock = program.initFunc.body.getLast();
            curBlock.addInstr(new RetInstr(curBlock, null));
            curBlock = program.mainFunc.body.getFirst();
            curBlock.addInstr(0, new CallInstr(curBlock, null, ".init", new ArrayList<>()));
        }
    }

    public void visit(ClassBuildNode node) {
        ArrayList<IRLocalVar> params = new ArrayList<>();
        params.add(new IRLocalVar("this", new IRType("ptr")));
        var constructor = new FuncDefMod(new IRType("void"), String.format("class.%s.%s", curScope.className, curScope.className), params);
        program.funcDefs.add(constructor);
        curBlock = constructor.body.getFirst();
        curScope = new IRScope(curScope);
        curScope.addVar("this", 1);
        constructor.defineLocalVar("this", 1);
        var thisPtr = new IRLocalVar("this.1", new IRType("ptr"));
        curBlock.addInstr(new AllocaInstr(curBlock, thisPtr, new IRType("ptr")));
        curBlock.addInstr(new StoreInstr(curBlock, params.getFirst(), thisPtr));
        node.body.accept(this);
        curScope = curScope.parent;
        curBlock = null;
    }

    public void visit(ClassDefNode node) {
        ArrayList<IRType> memTypes = new ArrayList<>();
        int size = 0;
        for (var varDef : node.varDefList) {
            int varNum = varDef.vars.size();
            for (int i = 0; i < varNum; ++i)
                memTypes.add(new IRType(varDef.type));
            if (varDef.type.dim > 0 || varDef.type.isClass || varDef.type.isString || varDef.type.isInt) size += 4 * varNum;
            else if (varDef.type.isBool) size += varNum;
        }
        gScope.setClassSize(node.className, size);
        program.structDefs.add(new StructDefMod(node.className, memTypes));
        curScope = new IRScope(curScope, node.className);
        if (node.classBuild != null)
            node.classBuild.accept(this);
        for (var method : node.methodDefList)
            method.accept(this);
        curScope = curScope.parent;
    }

    public void visit(FuncDefNode node) {
        ArrayList<IRLocalVar> params = new ArrayList<>();
        var func = new FuncDefMod(new IRType(node.type), node.funcName, params);
        if (curScope.className != null) {
            func.funcName = String.format("class.%s.%s", curScope.className, node.funcName);
            params.add(new IRLocalVar("this", new IRType("ptr")));
        }
        program.funcDefs.add(func);
        curScope = new IRScope(curScope);
        curBlock = func.body.getFirst();
        for (var param : node.paramList) {
            var irParam = new IRLocalVar(param.b, new IRType(param.a));
            params.add(irParam);
            int no = func.getLocalVarNo(param.b);
            func.defineLocalVar(param.b, ++no);
            curScope.addVar(param.b, no);
            var paramPtr = new IRLocalVar(param.b, new IRType(param.a));
            curBlock.addInstr(new AllocaInstr(curBlock, paramPtr, new IRType(param.a)));
            curBlock.addInstr(new StoreInstr(curBlock, irParam, paramPtr));
        }
        node.body.accept(this);
        curScope = curScope.parent;
        curBlock = null;
    }

    public void visit(SuiteNode node) {
        for (var stmt : node.stmts)
            stmt.accept(this);
    }

    public void visit(VarDefStmtNode node) {
        node.varDef.accept(this);
    }
    public void visit(ExprStmtNode node) {
        node.expr.accept(this);
    }
    public void visit(IfStmtNode node) {
        node.condition.accept(this);
        int ifNo =  curBlock.parent.ifCnt++;
        IRBlock thenBlock = new IRBlock(curBlock.parent, String.format("if_then.%d", ifNo));
        IRBlock elseBlock = new IRBlock(curBlock.parent, String.format("if_else.%d", ifNo));
        IRBlock endBlock = new IRBlock(curBlock.parent, String.format("if_end.%d", ifNo));
        if (lastExpr.value instanceof IRLiteral logic) {
            if (logic.value == 0) curBlock.addInstr(new BrInstr(curBlock, null, (node.elseStmt != null ? elseBlock : endBlock), null));
            else curBlock.addInstr(new BrInstr(curBlock, null, thenBlock, null));
        } else curBlock.addInstr(new BrInstr(curBlock, (IRLocalVar) lastExpr.value, thenBlock, (node.elseStmt != null ? elseBlock : endBlock)));
        curBlock = thenBlock;
        node.thenStmt.accept(this);
        curBlock.addInstr(new BrInstr(curBlock, null, endBlock, null));
        curBlock.parent.body.add(thenBlock);
        if (node.elseStmt != null) {
            curBlock = elseBlock;
            node.elseStmt.accept(this);
            curBlock.addInstr(new BrInstr(curBlock, null, endBlock, null));
            curBlock.parent.body.add(elseBlock);
        }
        curBlock = endBlock;
        curBlock.parent.body.add(endBlock);
    }
    public void visit(ForStmtNode node) {
        int loopNo = curBlock.parent.loopCnt++;
        IRBlock loopCond = new IRBlock(curBlock.parent, String.format("loop_cond.%d", loopNo));
        IRBlock loopBody = new IRBlock(curBlock.parent, String.format("loop_body.%d", loopNo));
        IRBlock loopStep = new IRBlock(curBlock.parent, String.format("loop_step.%d", loopNo));
        IRBlock loopEnd = new IRBlock(curBlock.parent, String.format("loop_end.%d", loopNo));
        curScope = new IRScope(curScope, loopEnd, loopStep);
        if (node.init != null)
            node.init.accept(this);
        curBlock = loopCond;
        if (node.cond != null) {
            node.accept(this);
            if (lastExpr.value instanceof IRLiteral logic) {
                if (logic.value == 0) curBlock.addInstr(new BrInstr(curBlock, null, loopEnd, null));
                else curBlock.addInstr(new BrInstr(curBlock, null, loopBody, null));
            } else curBlock.addInstr(new BrInstr(curBlock, (IRLocalVar) lastExpr.value, loopBody, loopEnd));
        } else curBlock.addInstr(new BrInstr(curBlock, null, loopBody, null));
        curBlock.parent.body.add(loopCond);
        curBlock = loopBody;
        node.body.accept(this);
        curBlock.addInstr(new BrInstr(curBlock, null, loopStep, null));
        curBlock.parent.body.add(loopBody);
        curBlock = loopStep;
        if (node.step != null)
            node.step.accept(this);
        curBlock.addInstr(new BrInstr(curBlock, null, loopCond, null));
        curBlock.parent.body.add(loopStep);
        curBlock = loopEnd;
        curBlock.parent.body.add(loopEnd);
        curScope = curScope.parent;
    }
    public void visit(WhileStmtNode node) {
        int loopNo = curBlock.parent.loopCnt++;
        IRBlock loopCond = new IRBlock(curBlock.parent, String.format("loop_cond.%d", loopNo));
        IRBlock loopBody = new IRBlock(curBlock.parent, String.format("loop_body.%d", loopNo));
        IRBlock loopEnd = new IRBlock(curBlock.parent, String.format("loop_end.%d", loopNo));
        curBlock = loopCond;
        node.condition.accept(this);
        if (lastExpr.value instanceof IRLiteral logic) {
            if (logic.value == 0) curBlock.addInstr(new BrInstr(curBlock, null, loopEnd, null));
            else curBlock.addInstr(new BrInstr(curBlock, null, loopBody, null));
        } else curBlock.addInstr(new BrInstr(curBlock, (IRLocalVar) lastExpr.value, loopBody, loopEnd));
        curBlock.parent.body.add(loopCond);
        curBlock = loopBody;
        curScope = new IRScope(curScope, loopEnd, loopCond);
        node.body.accept(this);
        curBlock.addInstr(new BrInstr(curBlock, null, loopCond, null));
        curScope = curScope.parent;
        curBlock.parent.body.add(loopBody);
        curBlock = loopEnd;
        curBlock.parent.body.add(loopEnd);
    }
    public void visit(ReturnStmtNode node) {
        if (node.returnValue != null) {
            node.returnValue.accept(this);
            if (lastExpr.value.type.isVoid) curBlock.addInstr(new RetInstr(curBlock, null));
            curBlock.addInstr(new RetInstr(curBlock, lastExpr.value));
        } else curBlock.addInstr(new RetInstr(curBlock, null));
    }
    public void visit(BreakStmtNode node) {
        curBlock.addInstr(new BrInstr(curBlock, null, curScope.loopEnd, null));
    }
    public void visit(ContinueStmtNode node) {
        curBlock.addInstr(new BrInstr(curBlock, null, curScope.loopNext, null));
    }
    public void visit(EmptyStmtNode node) {}
    public void visit(SuiteStmtNode node) {
        curScope = new IRScope(curScope);
        node.suite.accept(this);
        curScope = curScope.parent;
    }

    public void visit(NewArrayExprNode node) {}
    public void visit(NewEmptyArrayExprNode node) {}
    public void visit(NewTypeExprNode node) {
        var tmp = new IRLocalVar("tmp", new IRType("ptr"));
        ArrayList<IREntity> args = new ArrayList<>();
        if (gScope.hasOverrideConstructor(node.newType.baseTypename)) {
            args.add(tmp);
            curBlock.addInstr(new CallInstr(curBlock, null, String.format("class.%s.%s", node.newType.baseTypename, node.newType.baseTypename), args));
        } else {
            args.add(new IRLiteral(new IRType("i32"), 1));
            args.add(new IRLiteral(new IRType("i32"), gScope.getClassSize(node.newType.baseTypename)));
            curBlock.addInstr(new CallInstr(curBlock, tmp, ".builtin.calloc", args));
        }
        lastExpr = new IRExpr(tmp);
    }
    public void visit(FuncCallExprNode node) {}
    public void visit(MemberExprNode node) {}
    public void visit(IndexExprNode node) {}
    public void visit(PreSelfExprNode node) {}
    public void visit(UnaryExprNode node) {}
    public void visit(BinaryExprNode node) {}
    public void visit(TernaryExprNode node) {}
    public void visit(AssignExprNode node) {}
    public void visit(AtomExprNode node) {
        if (node.isLiteral)
    }
    public void visit(FStringExprNode node) {}

    public void visit(VarDefNode node) {}

    public void visit(LiteralNode node) {
        if (node.constArray != null)
            node.constArray.accept(this);
    }
    public void visit(ConstArrayNode node) {}
}
