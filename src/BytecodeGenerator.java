import java.util.*;

public class BytecodeGenerator extends MiniJavaParserBaseVisitor<MiniJavaType>
        implements MiniJavaParserVisitor<MiniJavaType> {



    @Override
    public MiniJavaType visitCompilationUnit(MiniJavaParser.CompilationUnitContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public MiniJavaType visitBlock(MiniJavaParser.BlockContext ctx) {
        // symbolTableStack.add(new HashMap<>());
        var ret = visitChildren(ctx);
        // displaySymbolTable();
        // symbolTableStack.removeLast();
        return ret;
    }
}
