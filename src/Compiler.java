import AST.Program.ProgramNode;
import Frontend.ASTBuilder;
import Frontend.SemanticChecker;
import Frontend.SymbolCollector;
import Midend.IRBuilder;
import Parser.MxParser;
import Parser.MxLexer;
import Util.Scope.GlobalScope;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import Util.MxErrorListener;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Compiler {
    public static void main(String[] args) throws Exception {
//        String testcaseName = "sema", packageName = "basic", ind = "71";
//        InputStream input = new FileInputStream(STR."testcases/\{testcaseName}/\{packageName}-package/\{packageName}-\{ind}.mx");
//        InputStream input = new FileInputStream("testcases/codegen/e2.mx");
//        OutputStream output = new FileOutputStream("out.ll");
        InputStream input = System.in;
        OutputStream output = System.out;
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
            IRBuilder irBuilder = new IRBuilder(gScope);
            irBuilder.visit(astRoot);
            output.write(irBuilder.program.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Util.Error.Error error) {
            System.err.println(error.toString());
            System.out.println(error.errorType());
            System.exit(-1);
        }
    }
}
