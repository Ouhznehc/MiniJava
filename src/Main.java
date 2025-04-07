import java.io.File;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class Main {
    public static void run(String sourcePath) throws Exception {
        var input = CharStreams.fromFileName(sourcePath);
        MiniJavaLexer lexer = new MiniJavaLexer(input);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        MiniJavaParser parser = new MiniJavaParser(tokenStream);
        ParseTree pt = parser.compilationUnit();

        String bytecodePath = sourcePath.substring(0, sourcePath.length() - 2) + "bc";
        String poolsPath = sourcePath.substring(0, sourcePath.length() - 2) + "pool";

        BytecodeGenerator bytecode = new BytecodeGenerator();
        Environment environment = new Environment();

        SemanticsVisitor semanticsVisitor = new SemanticsVisitor();
        semanticsVisitor.visit(pt);

        BytecodeVisitor bytecodeVisitor = new BytecodeVisitor(bytecode, environment, semanticsVisitor);
        bytecodeVisitor.visit(pt);

        bytecode.displayBytecodes(bytecodePath);
        environment.displayEnvironment(poolsPath);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Error: One argument required: <source.mj>.");
            throw new RuntimeException("Incorrect number of arguments.");
        }
    
        String sourcePath = new File(args[0]).getAbsolutePath();
        if (!sourcePath.endsWith(".mj")) {
            throw new RuntimeException("Error: Source file must end with .mj.");
        }
        run(sourcePath);
    }
}