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
import IR.Module.GlobalVarDefMod;
import IR.Module.StringLiteralDefMod;
import IR.Module.StructDefMod;
import Util.IRObject.IREntity.*;
import Util.IRObject.IRExpression;
import Util.IRObject.IRFunction;
import Util.Scope.GlobalScope;
import Util.Scope.IRScope;
import Util.Type.IRType;
import Util.Type.Type;

import java.util.ArrayList;
import java.util.List;

// TODO IR转义字符

// optimize
// TODO AST常量表达式折叠 + IR全局变量常量不用init
// TODO return, break, continue后面的死代码删除

public class IRBuilder implements ASTVisitor {
    public GlobalScope gScope;
    public IRProgram program;
    public IRBlock curBlock;
    public IRScope curScope;
    public IRExpression lastExpr;

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
        for (var varDef : node.varDefList) {
            int varNum = varDef.vars.size();
            for (int i = 0; i < varNum; ++i)
                memTypes.add(new IRType(varDef.type));
        }
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
        curBlock.parent.body.add(thenBlock);
        node.thenStmt.accept(this);
        curBlock.addInstr(new BrInstr(curBlock, null, endBlock, null));
        if (node.elseStmt != null) {
            curBlock = elseBlock;
            curBlock.parent.body.add(elseBlock);
            node.elseStmt.accept(this);
            curBlock.addInstr(new BrInstr(curBlock, null, endBlock, null));
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
        curBlock.parent.body.add(loopCond);
        if (node.cond != null) {
            node.accept(this);
            if (lastExpr.value instanceof IRLiteral logic) {
                if (logic.value == 0) curBlock.addInstr(new BrInstr(curBlock, null, loopEnd, null));
                else curBlock.addInstr(new BrInstr(curBlock, null, loopBody, null));
            } else curBlock.addInstr(new BrInstr(curBlock, (IRLocalVar) lastExpr.value, loopBody, loopEnd));
        } else curBlock.addInstr(new BrInstr(curBlock, null, loopBody, null));
        curBlock = loopBody;
        curBlock.parent.body.add(loopBody);
        node.body.accept(this);
        curBlock.addInstr(new BrInstr(curBlock, null, loopStep, null));
        curBlock = loopStep;
        curBlock.parent.body.add(loopStep);
        if (node.step != null)
            node.step.accept(this);
        curBlock.addInstr(new BrInstr(curBlock, null, loopCond, null));
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
        curBlock.parent.body.add(loopCond);
        node.condition.accept(this);
        if (lastExpr.value instanceof IRLiteral logic) {
            if (logic.value == 0) curBlock.addInstr(new BrInstr(curBlock, null, loopEnd, null));
            else curBlock.addInstr(new BrInstr(curBlock, null, loopBody, null));
        } else curBlock.addInstr(new BrInstr(curBlock, (IRLocalVar) lastExpr.value, loopBody, loopEnd));
        curBlock = loopBody;
        curBlock.parent.body.add(loopBody);
        curScope = new IRScope(curScope, loopEnd, loopCond);
        node.body.accept(this);
        curBlock.addInstr(new BrInstr(curBlock, null, loopCond, null));
        curScope = curScope.parent;
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

    public void visit(NewArrayExprNode node) {
        node.constArray.accept(this);
        var newArr = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
        ArrayList<IREntity> args = new ArrayList<>();
        args.add(new IRLiteral(new IRType("i32"), 4));
        args.add(new IRLiteral(new IRType("i32"), node.type.dim));
        args.add(lastExpr.value);
        curBlock.addInstr(new CallInstr(curBlock, newArr, "array.copy", args));
        lastExpr = new IRExpression(newArr);
    }
    private IRLocalVar NewEmptyArray(int dim, ArrayList<IREntity> sizes) {
        var newArr = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
        ArrayList<IREntity> args = new ArrayList<>();
        args.add(new IRLiteral(new IRType("i32"), 4));
        args.add(sizes.get(dim));
        curBlock.addInstr(new CallInstr(curBlock, newArr, "array.calloc", args));
        if (dim < sizes.size() - 1) {
            int loopNo = curBlock.parent.loopCnt++;
            IRBlock loopCond = new IRBlock(curBlock.parent, String.format("loop_cond.%d", loopNo));
            IRBlock loopBody = new IRBlock(curBlock.parent, String.format("loop_body.%d", loopNo));
            IRBlock loopStep = new IRBlock(curBlock.parent, String.format("loop_step.%d", loopNo));
            IRBlock loopEnd = new IRBlock(curBlock.parent, String.format("loop_end.%d", loopNo));
            // init
            var iPtr = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
            curBlock.addInstr(new AllocaInstr(curBlock, iPtr, new IRType("i32")));
            curBlock.addInstr(new StoreInstr(curBlock, new IRLiteral(new IRType("i32"), 0), iPtr));
            // cond
            curBlock = loopCond;
            curBlock.parent.body.add(loopCond);
            var iValue = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("i32"));
            curBlock.addInstr(new LoadInstr(curBlock, iValue, iPtr));
            var cond = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("i1"));
            curBlock.addInstr(new IcmpInstr(curBlock, "slt", iValue, sizes.get(dim), cond));
            curBlock.addInstr(new BrInstr(curBlock, cond, loopBody, loopEnd));
            // body
            curBlock = loopBody;
            curBlock.parent.body.add(loopBody);
            var indPtr = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
            curBlock.addInstr(new GetelementptrInstr(curBlock, indPtr, "ptr", newArr, iValue));
            curBlock.addInstr(new StoreInstr(curBlock, NewEmptyArray(dim + 1, sizes), indPtr));
            curBlock.addInstr(new BrInstr(curBlock, null, loopStep, null));
            // step
            curBlock = loopStep;
            curBlock.parent.body.add(loopStep);
            var iStep = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("i32"));
            curBlock.addInstr(new BinaryInstr(curBlock, "add", iValue, new IRLiteral(new IRType("i32"), 1), iStep));
            curBlock.addInstr(new StoreInstr(curBlock, iStep, iPtr));
            curBlock.addInstr(new BrInstr(curBlock, null, loopCond, null));
            // end
            curBlock = loopEnd;
            curBlock.parent.body.add(loopEnd);
        }
        return newArr;
    }
    public void visit(NewEmptyArrayExprNode node) {
        ArrayList<IREntity> sizes = new ArrayList<>();
        for (var size : node.sizeList) {
            size.accept(this);
            sizes.add(lastExpr.value);
        }
        lastExpr = new IRExpression(NewEmptyArray(0, sizes));
    }
    public void visit(NewTypeExprNode node) {
        var tmp = new IRLocalVar("tmp", new IRType("ptr"));
        ArrayList<IREntity> args = new ArrayList<>();
        args.add(new IRLiteral(new IRType("i32"), 1));
        args.add(new IRLiteral(new IRType("i32"), gScope.getClassSize(node.newType.baseTypename)));
        curBlock.addInstr(new CallInstr(curBlock, tmp, ".builtin.calloc", args));
        if (gScope.hasOverrideConstructor(node.newType.baseTypename)) {
            args.clear();
            args.add(tmp);
            curBlock.addInstr(new CallInstr(curBlock, null, String.format("class.%s.%s", node.newType.baseTypename, node.newType.baseTypename), args));
        }
        lastExpr = new IRExpression(tmp);
    }
    public void visit(FuncCallExprNode node) {
        node.func.accept(this);
        var callInstr = new CallInstr(curBlock, null, lastExpr.func.funcName, new ArrayList<>());
        if (!lastExpr.func.returnType.isVoid)
            callInstr.result = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), lastExpr.func.returnType);
        for (var arg : node.args) {
            arg.accept(this);
            callInstr.args.add(lastExpr.value);
        }
        curBlock.addInstr(callInstr);
        if (!lastExpr.func.returnType.isVoid)
            lastExpr = new IRExpression(callInstr.result);
    }
    public void visit(MemberExprNode node) {
        node.classExpr.accept(this);
        if (node.classExpr.type.isString) {
            switch (node.identifier) {
                case "length" -> lastExpr = new IRExpression(new IRFunction("string.length", new IRType("i32"), (IRVariable) lastExpr.value));
                case "substring" -> lastExpr = new IRExpression(new IRFunction("string.substring", new IRType("ptr"), (IRVariable) lastExpr.value));
                case "parseInt" -> lastExpr = new IRExpression(new IRFunction("string.parseInt", new IRType("i32"), (IRVariable) lastExpr.value));
                case "ord" -> lastExpr = new IRExpression(new IRFunction("string.ord", new IRType("i32"), (IRVariable) lastExpr.value));
            }
        } else if (node.classExpr.type.dim > 0) {
            lastExpr = new IRExpression(new IRFunction("array.size", new IRType("i32"), (IRLocalVar) lastExpr.value));
        } else if (node.classExpr.type.isClass) {
            var memberType = gScope.getClassMember(node.classExpr.type, node.identifier);
            if (memberType != null) {
                var ptr = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
                var value = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType(memberType));
                curBlock.addInstr(new GetelementptrInstr(curBlock, ptr, String.format("%%class.%s", node.classExpr.type.baseTypename), (IRVariable) lastExpr.value,
                        new IRLiteral(new IRType("i32"), 0), new IRLiteral(new IRType("i32"), gScope.getClassMemberIndex(node.classExpr.type.baseTypename, node.identifier))));
                curBlock.addInstr(new LoadInstr(curBlock, value, ptr));
                lastExpr = new IRExpression(value, ptr);
            } else {
                var methodType = gScope.getClassMethod(node.classExpr.type, node.identifier);
                lastExpr = new IRExpression(new IRFunction(String.format("class.%s.%s", node.classExpr.type.baseTypename, node.identifier), new IRType(methodType)));
            }
        }
    }
    public void visit(IndexExprNode node) {
        node.array.accept(this);
        var tmpArr = (IRLocalVar) lastExpr.value;
        node.index.accept(this);
        var tmpInd = lastExpr.value;
        var tmpPtr = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
        var tmpValue = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType(node.type));
        curBlock.addInstr(new GetelementptrInstr(curBlock, tmpPtr, tmpValue.type.toString(), tmpArr, tmpInd));
        curBlock.addInstr(new LoadInstr(curBlock, tmpValue, tmpPtr));
        lastExpr = new IRExpression(tmpValue, tmpPtr);
    }
    public void visit(PreSelfExprNode node) {
        node.expr.accept(this);
        var tmp = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("i32"));
        curBlock.addInstr(new BinaryInstr(curBlock, (node.isIncrement ? "add" : "sub"), lastExpr.value, new IRLiteral(new IRType("i32"), 1), tmp));
        curBlock.addInstr(new StoreInstr(curBlock, tmp, lastExpr.ptr));
        lastExpr = new IRExpression(tmp, lastExpr.ptr);
    }
    public void visit(UnaryExprNode node) {
        node.expr.accept(this);
        var tmp = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType(node.op.equals("!") ? "i1" : "i32"));
        switch (node.op) {
            case "!" -> curBlock.addInstr(new BinaryInstr(curBlock, "xor", lastExpr.value, new IRLiteral(new IRType("i1"), 1), tmp));
            case "-" -> curBlock.addInstr(new BinaryInstr(curBlock, "sub", new IRLiteral(new IRType("i32"), 0), lastExpr.value, tmp));
            case "~" -> curBlock.addInstr(new BinaryInstr(curBlock, "xor", lastExpr.value, new IRLiteral(new IRType("i32"), -1), tmp));
            case "++", "--" -> {
                curBlock.addInstr(new BinaryInstr(curBlock, (node.op.equals("++") ? "add" : "sub"), lastExpr.value, new IRLiteral(new IRType("i32"), 1), tmp));
                curBlock.addInstr(new StoreInstr(curBlock, tmp, lastExpr.ptr));
                lastExpr = new IRExpression(lastExpr.value);
                return;
            }
        }
        lastExpr = new IRExpression(tmp);
    }
    public void visit(BinaryExprNode node) {
        switch (node.op) {
            case "==", "!=", "<", "<=", ">", ">=" -> {
                node.lhs.accept(this);
                var lhs = lastExpr.value;
                node.rhs.accept(this);
                var rhs = lastExpr.value;
                var tmp = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("i1"));
                if (node.lhs.type.isString) {
                    ArrayList<IREntity> args = new ArrayList<>();
                    args.add(lhs);
                    args.add(rhs);
                    switch (node.op) {
                        case "==" -> curBlock.addInstr(new CallInstr(curBlock, tmp, "string.equal", args));
                        case "!=" -> curBlock.addInstr(new CallInstr(curBlock, tmp, "string.notEqual", args));
                        case "<" -> curBlock.addInstr(new CallInstr(curBlock, tmp, "string.less", args));
                        case "<=" -> curBlock.addInstr(new CallInstr(curBlock, tmp, "string.lessOrEqual", args));
                        case ">" -> curBlock.addInstr(new CallInstr(curBlock, tmp, "string.greater", args));
                        case ">=" -> curBlock.addInstr(new CallInstr(curBlock, tmp, "string.greaterOrEqual", args));
                    }
                } else curBlock.addInstr(new IcmpInstr(curBlock, IcmpInstr.getCond(node.op), lhs, rhs, tmp));
                lastExpr = new IRExpression(tmp);
            }
            case "&&", "||" -> {
                var ptr = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
                curBlock.addInstr(new AllocaInstr(curBlock, ptr, new IRType("i1")));
                int no = curBlock.parent.andOrCnt++;
                IRBlock rhsBlock = new IRBlock(curBlock.parent, String.format("and_or_rhs.%d", no));
                IRBlock endBlock = new IRBlock(curBlock.parent, String.format("and_or_end.%d", no));
                node.lhs.accept(this);
                var lhs = lastExpr.value;
                curBlock.addInstr(new StoreInstr(curBlock, lhs, ptr));
                if (node.op.equals("&&")) curBlock.addInstr(new BrInstr(curBlock, (IRLocalVar) lhs, rhsBlock, endBlock));
                else curBlock.addInstr(new BrInstr(curBlock, (IRLocalVar) lhs, endBlock, rhsBlock));
                curBlock = rhsBlock;
                curBlock.parent.body.add(rhsBlock);
                node.rhs.accept(this);
                var tmpResult = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("i1"));
                curBlock.addInstr(new BinaryInstr(curBlock, BinaryInstr.getOp(node.op), lhs, lastExpr.value, tmpResult));
                curBlock.addInstr(new StoreInstr(curBlock, tmpResult, ptr));
                curBlock = endBlock;
                curBlock.parent.body.add(endBlock);
                var tmp = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("i1"));
                curBlock.addInstr(new LoadInstr(curBlock, tmp, ptr));
                lastExpr = new IRExpression(tmp);
            }
            case "&", "|", "^", "<<", ">>", "+", "-", "*", "/", "%" -> {
                node.lhs.accept(this);
                var lhs = lastExpr.value;
                node.rhs.accept(this);
                var rhs = lastExpr.value;
                var tmp = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType(node.lhs.type.isString ? "ptr" : "i32"));
                if (node.lhs.type.isString) {
                    ArrayList<IREntity> args = new ArrayList<>();
                    args.add(lhs);
                    args.add(rhs);
                    curBlock.addInstr(new CallInstr(curBlock, tmp, "string.add", args));
                } else curBlock.addInstr(new BinaryInstr(curBlock, BinaryInstr.getOp(node.op), lhs, rhs, tmp));
                lastExpr = new IRExpression(tmp);
            }
        }
    }
    public void visit(TernaryExprNode node) {
        node.condition.accept(this);
        int ternaryNo = curBlock.parent.ternaryCnt++;
        IRBlock thenBlock = new IRBlock(curBlock.parent, String.format("ternary_then.%d", ternaryNo));
        IRBlock elseBlock = new IRBlock(curBlock.parent, String.format("ternary_else.%d", ternaryNo));
        IRBlock endBlock = new IRBlock(curBlock.parent, String.format("ternary_end.%d", ternaryNo));
        if (lastExpr.value instanceof IRLiteral logic) {
            if (logic.value == 0) curBlock.addInstr(new BrInstr(curBlock, null, elseBlock, null));
            else curBlock.addInstr(new BrInstr(curBlock, null, thenBlock, null));
        } else curBlock.addInstr(new BrInstr(curBlock, (IRLocalVar) lastExpr.value, thenBlock, elseBlock));
        // TODO 用select还是store+load？
        var cond = lastExpr.value;
        curBlock = thenBlock;
        curBlock.parent.body.add(thenBlock);
        node.thenExpr.accept(this);
        curBlock.addInstr(new BrInstr(curBlock, null, endBlock, null));
        var thenValue = lastExpr.value;
        curBlock = elseBlock;
        curBlock.parent.body.add(elseBlock);
        node.elseExpr.accept(this);
        curBlock.addInstr(new BrInstr(curBlock, null, endBlock, null));
        var elseValue = lastExpr.value;
        curBlock = endBlock;
        curBlock.parent.body.add(endBlock);
        var tmp = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType(node.type));
        curBlock.addInstr(new SelectInstr(curBlock, tmp, cond, thenValue, elseValue));
        lastExpr = new IRExpression(tmp);
    }
    public void visit(AssignExprNode node) {
        node.lhs.accept(this);
        var ptr = lastExpr.ptr;
        node.rhs.accept(this);
        var value = lastExpr.value;
        curBlock.addInstr(new StoreInstr(curBlock, value, ptr));
        // 使用赋值表达式的值是未定义的，所以没有lastExpr
    }
    private IRGlobalPtr NewStringLiteral(String value) {
        var ptr = new IRGlobalPtr(String.format(".str.%d", program.stringLiteralDefs.size()), new IRType("ptr"));
        program.stringLiteralDefs.add(new StringLiteralDefMod(value, ptr));
        return ptr;
    }
    public void visit(AtomExprNode node) {
        if (node.isLiteral) {
            if (node.literal.type.isNull) lastExpr = new IRExpression(new IRLiteral(new IRType("ptr"), true));
            else if (node.literal.type.dim > 0) node.literal.constArray.accept(this);
            else if (node.literal.type.isString) lastExpr = new IRExpression(NewStringLiteral(node.literal.constString));
            else if (node.literal.type.isInt) lastExpr = new IRExpression(new IRLiteral(new IRType("i32"), node.literal.constInt));
            else if (node.literal.type.isBool) lastExpr = new IRExpression(new IRLiteral(new IRType("i1"), node.literal.constLogic ? 1 : 0));
        } else if (node.isIdentifier) {
            if (node.type.isFunc) {
                if (curScope != null && curScope.className != null && gScope.getClassMethod(new Type(curScope.className, 0), node.identifier) != null) {
                    var thisPtr = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
                    curBlock.addInstr(new LoadInstr(curBlock, thisPtr, new IRLocalVar("this.1", new IRType("ptr"))));
                    lastExpr = new IRExpression(new IRFunction(String.format("class.%s.%s", curScope.className, node.identifier), new IRType(node.type.funcDecl.type), thisPtr));
                } else lastExpr = new IRExpression(new IRFunction(node.identifier, new IRType(node.type.funcDecl.type)));
                return;
            }
            IRVariable ptr;
            if (curScope != null && curScope.getVarNo(node.identifier) > 0) {
                ptr = new IRLocalVar(String.format("%s.%d", node.identifier, curScope.getVarNo(node.identifier)), new IRType("ptr"));
            } else if (curScope != null && curScope.className != null) {
                var thisPtr = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
                ptr = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
                curBlock.addInstr(new LoadInstr(curBlock, thisPtr, new IRLocalVar("this.1", new IRType("ptr"))));
                curBlock.addInstr(new GetelementptrInstr(curBlock, (IRLocalVar) ptr, String.format("%%class.%s", curScope.className), thisPtr,
                        new IRLiteral(new IRType("i32"), 0), new IRLiteral(new IRType("i32"), gScope.getClassMemberIndex(curScope.className, node.identifier))));
            } else ptr = new IRGlobalPtr(node.identifier, new IRType("ptr"));
            var value = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType(node.type));
            curBlock.addInstr(new LoadInstr(curBlock, value, ptr));
            lastExpr = new IRExpression(value, ptr);
        } else {
            var thisPtr = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
            curBlock.addInstr(new LoadInstr(curBlock, thisPtr, new IRLocalVar("this.1", new IRType("ptr"))));
            lastExpr = new IRExpression(thisPtr);
        }
    }
    public void visit(FStringExprNode node) {
        if (node.isExpr) {
            ArrayList<IREntity> expressions = new ArrayList<>();
            for (var expr : node.exprList) {
                expr.b.accept(this);
                expressions.add(lastExpr.value);
            }
            var tmpFront = NewStringLiteral(node.front);
            var tmp = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
            if (expressions.getFirst().type.isSameType(new IRType("i1"))) {
                // bool: 1->true, 0->false
                var tmpBool = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
                curBlock.addInstr(new CallInstr(curBlock, tmpBool, ".builtin.boolToString", new ArrayList<>(List.of(expressions.getFirst()))));
                curBlock.addInstr(new CallInstr(curBlock, tmp, "string.add", new ArrayList<>(List.of(tmpFront, tmpBool))));
            } else curBlock.addInstr(new CallInstr(curBlock, tmp, "string.add", new ArrayList<>(List.of(tmpFront, expressions.getFirst()))));
            var lastTmp = tmp;
            for (int i = 1; i < expressions.size(); i++) {
                tmp = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
                var tmpMid = NewStringLiteral(node.exprList.get(i).a);
                curBlock.addInstr(new CallInstr(curBlock, tmp, "string.add", new ArrayList<>(List.of(lastTmp, tmpMid))));
                lastTmp = tmp;
                tmp = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
                curBlock.addInstr(new CallInstr(curBlock, tmp, "string.add", new ArrayList<>(List.of(lastTmp, expressions.get(i)))));
                lastTmp = tmp;
            }
            var tmpBack = NewStringLiteral(node.back);
            tmp = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
            curBlock.addInstr(new CallInstr(curBlock, tmp, "string.add", new ArrayList<>(List.of(lastTmp, tmpBack))));
            lastExpr = new IRExpression(tmp);
        } else lastExpr = new IRExpression(NewStringLiteral(node.front));
    }

    public void visit(VarDefNode node) {
        for (var varUnit : node.vars) {
            if (curScope == null) {
                var newVar = new IRGlobalPtr(varUnit.a, new IRType(node.type));
                program.globalVarDefs.add(new GlobalVarDefMod(newVar,
                        ((node.type.isInt || node.type.isBool) ? new IRLiteral(new IRType("i32"), 0) : new IRLiteral(new IRType("ptr"), true))));
                if (varUnit.b != null) {
                    curBlock = program.initFunc.body.getLast();
                    varUnit.b.accept(this);
                    curBlock.addInstr(new StoreInstr(curBlock, lastExpr.value, newVar));
                    curBlock = null;
                }
            } else {
                int no = curBlock.parent.getLocalVarNo(varUnit.a);
                curBlock.parent.defineLocalVar(varUnit.a, ++no);
                curScope.parent.addVar(varUnit.a, no);
                var newVar = new IRLocalVar(String.format("%s.%d", varUnit.a, no), new IRType("ptr"));
                curBlock.addInstr(new AllocaInstr(curBlock, newVar, new IRType(node.type)));
                if (varUnit.b != null) {
                    varUnit.b.accept(this);
                    curBlock.addInstr(new StoreInstr(curBlock, lastExpr.value, newVar));
                } else curBlock.addInstr(new StoreInstr(curBlock,
                        ((node.type.isInt || node.type.isBool) ? new IRLiteral(new IRType("i32"), 0) : new IRLiteral(new IRType("ptr"), true)), newVar));
            }
        }
    }

    public void visit(LiteralNode node) {}
    private IRLocalVar NewConstArray(ConstArrayNode node) {
        var newArr = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
        ArrayList<IREntity> args = new ArrayList<>();
        args.add(new IRLiteral(new IRType("i32"), 4));
        args.add(new IRLiteral(new IRType("i32"), node.constArray.size()));
        curBlock.addInstr(new CallInstr(curBlock, newArr, "array.calloc", args));
        if (node.arrayType.dim > 1) {
            for (int i = 0; i < node.constArray.size(); i++) {
                var ptr = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
                if (!node.constArray.get(i).type.isNull) {
                    curBlock.addInstr(new GetelementptrInstr(curBlock, ptr, "ptr", newArr, new IRLiteral(new IRType("i32"), i)));
                    curBlock.addInstr(new StoreInstr(curBlock, NewConstArray(node.constArray.get(i).constArray), ptr));
                }
            }
        } else {
            for (int i = 0; i < node.constArray.size(); i++) {
                var ptr = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
                curBlock.addInstr(new GetelementptrInstr(curBlock, ptr, "ptr", newArr, new IRLiteral(new IRType("i32"), i)));
                curBlock.addInstr(new StoreInstr(curBlock, new IRLiteral(node.constArray.get(i)), ptr));
            }
        }
        return newArr;
    }
    public void visit(ConstArrayNode node) {
        var ptr = new IRGlobalPtr(String.format(".arr.%d", program.constArrayCnt++), new IRType("ptr"));
        program.globalVarDefs.add(new GlobalVarDefMod(ptr, new IRLiteral(new IRType("ptr"), true)));
        var originBlock = curBlock;
        curBlock = program.initFunc.body.getLast();
        curBlock.addInstr(new StoreInstr(curBlock, NewConstArray(node), ptr));
        curBlock = originBlock;
        var tmp = new IRLocalVar(Integer.toString(curBlock.parent.anonymousVarCnt++), new IRType("ptr"));
        curBlock.addInstr(new LoadInstr(curBlock, tmp, ptr));
        lastExpr = new IRExpression(tmp);
    }
}
