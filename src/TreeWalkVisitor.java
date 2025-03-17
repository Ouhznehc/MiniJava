public class TreeWalkVisitor extends MiniJavaParserBaseVisitor<MiniJavaType> {

    private final Environment environment = new Environment();
    private final BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

    public void displayBytecodes() {
        bytecodeGenerator.displayBytecodes();
    }

    @Override
    public MiniJavaType visitCompilationUnit(MiniJavaParser.CompilationUnitContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public MiniJavaType visitBlock(MiniJavaParser.BlockContext ctx) {
//        symbolTableStack.add(new HashMap<>());
        var ret = visitChildren(ctx);
//        displaySymbolTable();
//        symbolTableStack.removeLast();
        return ret;
    }
}
