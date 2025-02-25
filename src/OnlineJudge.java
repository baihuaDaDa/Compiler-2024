import AST.Program.ProgramNode;
import Backend.ASMBuilder;
import Backend.ASMOptimizer.LinearScanner;
import Frontend.ASTBuilder;
import Frontend.SemanticChecker;
import Frontend.SymbolCollector;
import Midend.*;
import Midend.IROptimizer.DCE;
import Midend.IROptimizer.Global2Local;
import Midend.IROptimizer.Util.IRCFGBuilder;
import Midend.IROptimizer.Mem2Reg;
import Parser.MxParser;
import Parser.MxLexer;
import Util.Scope.GlobalScope;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import Util.MxErrorListener;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class OnlineJudge {
    public static void main(String[] args) throws Exception {
        InputStream input = System.in;
        InputStream builtin = new FileInputStream("src/Util/Builtin/builtin.s");
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
            IRCFGBuilder irCFGBuilder = new IRCFGBuilder(irBuilder.program);
            irCFGBuilder.build();
            Global2Local global2Local = new Global2Local(irBuilder.program);
            global2Local.run();
            Mem2Reg mem2Reg = new Mem2Reg(irBuilder.program);
            mem2Reg.run();
            DCE dce = new DCE(irBuilder.program);
            dce.run();
            LinearScanner linearScanner = new LinearScanner(irBuilder.program);
            linearScanner.run();
            ASMBuilder asmBuilder = new ASMBuilder();
            asmBuilder.visit(irBuilder.program);
            output.write(builtin.readAllBytes());
            output.write(asmBuilder.program.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Util.Error.Error error) {
            System.err.println(error.toString());
            System.out.println(error.errorType());
            System.exit(-1);
        }
    }
}
