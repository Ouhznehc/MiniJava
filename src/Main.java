import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import java.io.File;
import java.io.FileOutputStream;

public class Main {
    public static void run(File sourceFile, File bytecodeFile) throws Exception {
        var input = CharStreams.fromFileName(sourceFile.getAbsolutePath());
        MiniJavaLexer lexer = new MiniJavaLexer(input);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        MiniJavaParser parser = new MiniJavaParser(tokenStream);
        ParseTree pt = parser.compilationUnit();

        // Implement your own Visitor by extending MiniJavaParserBaseVisitor
        // For example, BytecodeGenerator should generate bytecode and provide a getBytecode() method
        BytecodeGenerator generator = new BytecodeGenerator();
        generator.visit(pt);

//        byte[] bytecode = generator.getBytecode();
//        try (FileOutputStream fos = new FileOutputStream(bytecodeFile)) {
//            fos.write(bytecode);
//        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Error: Two arguments required: <source.mj> <bytecode.bc>.");
            throw new RuntimeException("Incorrect number of arguments.");
        }

        File sourceFile = new File(args[0]);
        File bytecodeFile = new File(args[1]);
        run(sourceFile, bytecodeFile);
    }
}