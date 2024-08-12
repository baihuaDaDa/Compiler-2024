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
import Util.Decl.FuncDecl;
import Util.Error.SemanticError;
import Util.Scope.GlobalScope;
import Util.Scope.Scope;
import Util.Type.BaseType;
import Util.Type.ExprType;
import Util.Type.ReturnType;
import Util.Type.Type;

// TODO 数组类型的内建方法size()

public class SemanticChecker implements ASTVisitor {
    GlobalScope gScope;
    Scope curScope;

    public SemanticChecker(GlobalScope gScope) {
        this.gScope = gScope;
        this.curScope = gScope;
    }

    public void visit(ProgramNode node) {
        ExprType mainFunc = gScope.getFunc("main");
        if (mainFunc == null)
            throw new SemanticError("No main function", node.pos);
        if (!mainFunc.funcDecl.type.isSameType(new ReturnType("int", 0)))
            throw new SemanticError("Main function should return int", node.pos);
        if (!mainFunc.funcDecl.paramTypes.isEmpty())
            throw new SemanticError("Main function should not have parameters", node.pos);
        for (var classDef : node.classDefs)
            classDef.accept(this);
        for (var funcDef : node.funcDefs)
            funcDef.accept(this);
        for (var varDef : node.varDefs)
            varDef.accept(this);
    }

    public void visit(ClassBuildNode node) {
        if (!curScope.isInClass)
            throw new SemanticError("Class constructor outside class", node.pos);
        if (!curScope.classType.baseTypename.equals(node.className))
            throw new SemanticError("Wrong constructor", node.pos);
        curScope = new Scope(curScope, new ReturnType("void", 0));
        node.body.accept(this);
        curScope = curScope.getParent();
    }

    public void visit(ClassDefNode node) {
        curScope = new Scope(curScope, new BaseType(node.className));
        for (var varDef : node.varDefList)
            varDef.accept(this);
        for (var methodDef : node.methodDefList)
            methodDef.accept(this);
        curScope = curScope.getParent();
    }

    public void visit(FuncDefNode node) {
        if (node.type.isClass && !gScope.isClassDefined(node.type.baseTypename))
            throw new SemanticError("Class " + node.type.baseTypename + " not defined", node.pos);
        curScope = new Scope(curScope, node.type);
        for (var param : node.paramList) {
            if (param.a.isClass && !gScope.isClassDefined(param.a.baseTypename))
                throw new SemanticError("Class " + param.a.baseTypename + " not defined", node.pos);
            curScope.defineVar(param.b, param.a, node.pos);
        }
        node.body.accept(this);
        if (!curScope.isReturned && !node.funcName.equals("main"))
            throw new SemanticError("Function " + node.funcName + " have not returned yet", node.pos);
    }

    public void visit(VarDefStmtNode node) {
        node.varDef.accept(this);
    }
    public void visit(ExprStmtNode node) {
        node.expr.accept(this);
    }
    public void visit(IfStmtNode node) {
        node.condition.accept(this);
        if (!node.condition.type.isSameType(new ExprType("bool", 0)))
            throw new SemanticError("Condition of if statement is not bool", node.pos);
        node.thenStmt.accept(this);
        if (node.elseStmt != null)
            node.elseStmt.accept(this);
    }
    public void visit(ForStmtNode node) {
        curScope = new Scope(curScope, node.pos);
        if (node.init != null)
            node.init.accept(this);
        if (node.cond != null) {
            node.cond.accept(this);
            if (!node.cond.type.isSameType(new ExprType("bool", 0)))
                throw new SemanticError("Condition of for statement is not bool", node.pos);
        }
        if (node.step != null)
            node.step.accept(this);
        node.body.accept(this);
        curScope = curScope.getParent();
    }
    public void visit(WhileStmtNode node) {
        node.condition.accept(this);
        if (!node.condition.type.isSameType(new ExprType("bool", 0)))
            throw new SemanticError("Condition of while statement is not bool", node.pos);
        curScope = new Scope(curScope, node.pos);
        node.body.accept(this);
        curScope = curScope.getParent();
    }
    public void visit(ReturnStmtNode node) {
        if (!curScope.isInFunc)
            throw new SemanticError("Return outside function", node.pos);
        node.returnValue.accept(this);
        if (!curScope.returnType.isSameType(node.returnValue.type))
            throw new SemanticError("Return the type " + node.returnValue.type.toString() + " for the required type " + curScope.returnType.toString(), node.pos);
        curScope.isReturned = true;
    }
    public void visit(BreakStmtNode node) {
        if (!curScope.isInLoop)
            throw new SemanticError("Break outside loop", node.pos);
    }
    public void visit(ContinueStmtNode node) {
        if (!curScope.isInLoop)
            throw new SemanticError("Continue outside loop", node.pos);
    }
    public void visit(SuiteStmtNode node) {
        if (curScope.isFuncScope)
            curScope = new Scope(curScope);
        for (var stmt : node.stmts)
            stmt.accept(this);
        if (curScope.isFuncScope)
            curScope = curScope.getParent();
    }

    public void visit(NewArrayExprNode node) {}
    public void visit(NewEmptyArrayExprNode node) {}
    public void visit(NewTypeExprNode node) {}
    public void visit(FuncCallExprNode node) {
        node.func.accept(this);
        if (!node.func.type.isFunc)
            throw new SemanticError("Call a non-function expression", node.pos);
        if (node.args.size() != node.func.type.funcDecl.paramTypes.size())
            throw new SemanticError("The number of arguments should be " + node.func.type.funcDecl.paramTypes.size() + " instead of " + node.args.size(), node.pos);
        for (var arg : node.args) {
            arg.accept(this);
            int ind = node.args.indexOf(arg);
            if (!arg.type.isSameType(node.func.type.funcDecl.paramTypes.get(ind)))
                throw new SemanticError("The type of No." + ind + "argument should be " + node.func.type.funcDecl.paramTypes.get(ind).toString() + " instead of " + arg.type.toString(), node.pos);
        }
        node.type = new ExprType(node.func.type.funcDecl.type);
        node.isLeftValue = false;
    }
    public void visit(MemberExprNode node) {
        node.classExpr.accept(this);
        if (!node.classExpr.type.isClass && node.classExpr.type.dim > 0)
            throw new SemanticError("Invoke members or methods from a non-class expression", node.pos);
        Type memberType = gScope.getClassMember(node.classExpr.type.baseTypename, node.memberName);
        if (memberType == null)
            throw new SemanticError("Class " + node.classExpr.type.baseTypename + " not defined", node.pos);
        node.type = (ExprType) memberType;
        node.isLeftValue = true;
    }
    public void visit(IndexExprNode node) {
        node.array.accept(this);
        if (node.array.type.dim == 0)
            throw new SemanticError("Cannot visit any element of a non-array expression", node.pos);
        node.index.accept(this);
        if (!node.index.type.isSameType(new ExprType("int", 0)))
            throw new SemanticError("Type of index of array should be int instead of " + node.index.type.toString(), node.pos);
        node.type = new ExprType(node.array.type);
        node.type.dim--;
        node.isLeftValue = true;
    }
    public void visit(PreSelfExprNode node) {
        node.expr.accept(this);
        if (!node.expr.isLeftValue)
            throw new SemanticError("Right value cannot increment or decrement", node.pos);
        if (!node.expr.type.isSameType(new ExprType("int", 0)))
            throw new SemanticError("Type of pre-self expression should be int instead of " + node.expr.type.toString(), node.pos);
        node.type = new ExprType("int", 0);
        node.isLeftValue = true;
    }
    public void visit(UnaryExprNode node) {
        node.expr.accept(this);
        switch (node.op) {
            case "++", "--" -> {
                if (!node.expr.isLeftValue)
                    throw new SemanticError("Right value cannot increment or decrement", node.pos);
                if (!node.expr.type.isSameType(new ExprType("int", 0)))
                    throw new SemanticError("Type of suc-self expression should be int instead of " + node.expr.type.toString(), node.pos);
                node.type = new ExprType("int", 0);
            }
            case "+", "-", "!" -> {
                if (!node.expr.type.isSameType(new ExprType("int", 0)))
                    throw new SemanticError("Type of !, ~, - expression should be int instead of " + node.expr.type.toString(), node.pos);
                node.type = new ExprType("int", 0);
            }
        }
        node.isLeftValue = false;
    }
    public void visit(BinaryExprNode node) {
        node.lhs.accept(this);
        node.rhs.accept(this);
        if (!node.lhs.type.isSameType(node.rhs.type))
            throw new SemanticError("Type of lhs and rhs of binary expression should be same instead of " + node.lhs.type.toString() + " and " + node.rhs.type.toString(), node.pos);
    }
    public void visit(TernaryExprNode node) {
        node.condition.accept(this);
        if (!node.condition.type.isSameType(new ExprType("bool", 0)))
            throw new SemanticError("Type of condition of ternary expression should be bool instead of " + node.condition.type.toString(), node.pos);
        node.thenExpr.accept(this);
        node.elseExpr.accept(this);
        if (!node.thenExpr.type.isSameType(node.elseExpr.type))
            throw new SemanticError("Type of thenExpr and elseExpr of ternary expression should be same instead of " + node.thenExpr.type.toString() + " and " + node.elseExpr.type.toString(), node.pos);
        node.type = new ExprType(node.thenExpr.type);
        node.isLeftValue = false;
    }
    public void visit(AssignExprNode node) {
        node.lhs.accept(this);
        node.rhs.accept(this);
        if (!node.lhs.isLeftValue)
            throw new SemanticError("Right value cannot be assigned", node.pos);
        if (!node.lhs.type.isSameType(node.rhs.type))
            throw new SemanticError("Cannot assign type " + node.rhs.type.toString() + " to " + node.lhs.type.toString(), node.pos);
        node.type = new ExprType(node.lhs.type);
        node.isLeftValue = true;
    }
    public void visit(AtomExprNode node) {
        if (node.isLiteral) {
            node.type = new ExprType(node.literal.type);
            node.isLeftValue = false;
        } else if (node.isIdentifier) {
            Type varType = curScope.getVar(node.identifier);
            if (varType != null) {
                node.type = new ExprType(varType);
                node.isLeftValue = true;
            } else {
                ExprType funcType = gScope.getFunc(node.identifier);
                if (funcType == null)
                    throw new SemanticError("Identifier " + node.identifier + " not defined", node.pos);
                node.type = funcType;
                node.isLeftValue = false;
            }
        } else if (node.isThis) {
            if (!curScope.isInClass)
                throw new SemanticError("Invoke this outside class", node.pos);
            node.type = new ExprType(curScope.classType);
        }
    }
    public void visit(FStringExprNode node) {
        node.type = new ExprType("string", 0);
        for (var expr : node.exprList) {
            expr.b.accept(this);
            if (!expr.b.type.isSameType(new ExprType("string", 0))
                    && !expr.b.type.isSameType(new ExprType("int", 0))
                    && !expr.b.type.isSameType(new ExprType("bool", 0)))
                throw new SemanticError("Expression in fstring is " + expr.b.type.toString() + " instead of string, int or bool", node.pos);
        }
    }

    public void visit(VarDefNode node) {
        if (node.type.isClass && !gScope.isClassDefined(node.type.baseTypename))
            throw new SemanticError("Class " + node.type.baseTypename + " not defined", node.pos);
        for (var varUnit : node.vars) {
            if (curScope.isInGlobalClass && varUnit.b != null)
                throw new SemanticError("Class member" + varUnit.a + "cannot have initializer", node.pos);
            if (varUnit.b != null) {
                varUnit.b.accept(this);
                if (varUnit.b.type.isSameType(node.type))
                    throw new SemanticError("Variable (type: " + node.type.toString() + ") is initialized with wrong type" + varUnit.b.type.toString(), node.pos);
            }
            if (!curScope.isInClass)
                curScope.defineVar(varUnit.a, node.type, node.pos);
        }
    }

    public void visit(LiteralNode node) {}
    public void visit(ConstArrayNode node) {}
}
