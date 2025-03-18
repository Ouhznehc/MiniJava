import java.io.File;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class Main {
    public static void run(File sourceFile) throws Exception {
        var input = CharStreams.fromFileName(sourceFile.getAbsolutePath());
        MiniJavaLexer lexer = new MiniJavaLexer(input);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        MiniJavaParser parser = new MiniJavaParser(tokenStream);
        ParseTree pt = parser.compilationUnit();

        TreeWalkVisitor visitor = new TreeWalkVisitor();
        visitor.visit(pt);

        String sourcePath = sourceFile.getAbsolutePath();

        String bytecodePath, poolsPath;
        if (sourcePath.endsWith(".mj")) {
            bytecodePath = sourcePath.substring(0, sourcePath.length() - 2) + "bc";
            poolsPath = sourcePath.substring(0, sourcePath.length() - 2) + "pool";

        } else {
            throw new RuntimeException("Source file must end with .mj.");
        }
        
        visitor.displayBytecodes(bytecodePath);
        visitor.displayEnvironment(poolsPath);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Error: One argument required: <source.mj>.");
            throw new RuntimeException("Incorrect number of arguments.");
        }
    
        File sourceFile = new File(args[0]);
        run(sourceFile);
    }
}