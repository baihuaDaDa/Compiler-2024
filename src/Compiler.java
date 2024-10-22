import AST.Program.ProgramNode;
import Backend.ASMBuilder;
import Backend.LinearScanner;
import Frontend.ASTBuilder;
import Frontend.SemanticChecker;
import Frontend.SymbolCollector;
import Midend.DCE;
import Midend.IRBuilder;
import Midend.IRCFGBuilder;
import Midend.Mem2Reg;
import Parser.MxParser;
import Parser.MxLexer;
import Util.Scope.GlobalScope;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import Util.MxErrorListener;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Compiler {
    public static void main(String[] args) throws Exception {
        InputStream input = System.in;
        OutputStream output = System.out;
        OutputStream foutput = new FileOutputStream("test-script.s");
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
            IRCFGBuilder irCFGBuilder = new IRCFGBuilder(irBuilder.program);
            irCFGBuilder.build();
            Mem2Reg mem2Reg = new Mem2Reg(irBuilder.program);
            mem2Reg.run();
            DCE dce = new DCE(irBuilder.program);
            dce.run();
            LinearScanner linearScanner = new LinearScanner(irBuilder.program);
            linearScanner.run();
            ASMBuilder asmBuilder = new ASMBuilder();
            asmBuilder.visit(irBuilder.program);
            foutput.write(asmBuilder.program.toString().getBytes(StandardCharsets.UTF_8));
            output.write(asmBuilder.program.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Util.Error.Error error) {
            System.err.println(error.toString());
            System.out.println(error.errorType());
            System.exit(-1);
        }
    }
}
