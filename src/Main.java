import AST.Program.ProgramNode;
import Frontend.ASTBuilder;
import Frontend.SemanticChecker;
import Frontend.SymbolCollector;
import Parser.MxParser;
import Parser.MxLexer;
import Util.Scope.GlobalScope;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import Util.MxErrorListener;

import java.io.FileInputStream;
import java.io.InputStream;

public class Main {
    public static void main(String[] args) throws Exception {
        String testcaseName = "sema", packageName = "class", ind = "16";
        InputStream input = new FileInputStream(STR."testcases/\{testcaseName}/\{packageName}-package/\{packageName}-\{ind}.mx");
//        InputStream input = System.in;
        try {
            MxLexer lexer = new MxLexer(CharStreams.fromStream(input));
            lexer.removeErrorListeners();
            lexer.addErrorListener(new MxErrorListener());
            MxParser parser = new MxParser(new CommonTokenStream(lexer));
            parser.removeErrorListeners();
            parser.addErrorListener(new MxErrorListener());
            ParseTree parseTreeRoot = parser.program();
            ASTBuilder astBuilder = new ASTBuilder();
            ProgramNode astRoot = (ProgramNode) astBuilder.visit(parseTreeRoot);
            GlobalScope gScope = new GlobalScope();
            astRoot.accept(new SymbolCollector(gScope));
            astRoot.accept(new SemanticChecker(gScope));
        } catch (Error error) {
            System.err.println(error.toString());
        }
    }
}
