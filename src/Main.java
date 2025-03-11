import AST.Program.ProgramNode;
import Backend.ASMBuilder;
import Backend.ASMOptimizer.LinearScanner;
import Frontend.ASTBuilder;
import Frontend.SemanticChecker;
import Frontend.SymbolCollector;
import Midend.*;
import Midend.IROptimizer.*;
import Midend.IROptimizer.Util.DomTreeBuilder;
import Midend.IROptimizer.Util.IRCFGBuilder;
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

public class Main {
    public static void main(String[] args) throws Exception {
//        String testcaseName = "sema", packageName = "basic", ind = "71";
//        InputStream input = new FileInputStream(STR."testcases/\{testcaseName}/\{packageName}-package/\{packageName}-\{ind}.mx");
        InputStream input = new FileInputStream("testcases/codegen/t57.mx");
        OutputStream outputIR = new FileOutputStream("test.ll");
        OutputStream outputIROrigin = new FileOutputStream("test-origin.ll");
        OutputStream outputBeforeADCE = new FileOutputStream("test-before-ADCE.ll");
        OutputStream outputASM = new FileOutputStream("test.s");
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
            outputIROrigin.write(irBuilder.program.toString().getBytes(StandardCharsets.UTF_8));
            IRCFGBuilder irCFGBuilder = new IRCFGBuilder(irBuilder.program);
            irCFGBuilder.build();
            Global2Local global2Local = new Global2Local(irBuilder.program);
            global2Local.run();
            DomTreeBuilder domTreeBuilder = new DomTreeBuilder(irBuilder.program);
            domTreeBuilder.build();
            Mem2Reg mem2Reg = new Mem2Reg(irBuilder.program);
            mem2Reg.run();
            DCE dce = new DCE(irBuilder.program);
            dce.run();
            Inline inline = new Inline(irBuilder.program, 1);
            inline.run(); // CFG already updated
            outputBeforeADCE.write(irBuilder.program.toString().getBytes(StandardCharsets.UTF_8));
            ADCE adce = new ADCE(irBuilder.program);
            adce.run();
            irCFGBuilder.build(); // 更新 CFG
//            domTreeBuilder.build(); // 更新 DomTree
//            SCCP sccp = new SCCP(irBuilder.program);
//            sccp.run();
//            irCFGBuilder.build(); // 更新 CFG
//            adce.run();
//            irCFGBuilder.build(); // 更新 CFG
//            domTreeBuilder.build(); // 更新 DomTree
//            GCM gcm = new GCM(irBuilder.program);
//            gcm.run();
//            adce.run();
//            irCFGBuilder.build(); // 更新 CFG
            outputIR.write(irBuilder.program.toString().getBytes(StandardCharsets.UTF_8));
            irCFGBuilder.build();
            domTreeBuilder.build();
            LinearScanner linearScanner = new LinearScanner(irBuilder.program);
            linearScanner.run();
            ASMBuilder asmBuilder = new ASMBuilder();
            asmBuilder.visit(irBuilder.program);
            outputASM.write(asmBuilder.program.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Util.Error.Error error) {
            System.err.println(error.toString());
            System.out.println(error.errorType());
            System.exit(-1);
        }
    }
}
