import java.util.HashMap;
import java.util.Map;

public class TreeWalkVisitor extends MiniJavaParserBaseVisitor<MiniJavaObject> {

    private final Environment environment = new Environment();
    private final BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

    private String curScope = "0_global";
    private Integer curLabel = 0;

    
    private MiniJavaObject findVariableByID(String id) {
        // First try to find the variable in the global scope
        var symbolTable = environment.symbolTable.get("0_global");
        for (var i = symbolTable.size() - 1; i >= 0; i--) {
            var symbol = symbolTable.get(i).get(id);
            if (symbol != null) {
                return symbol;
            }
        }
        // Then try to find the variable in the current scope
        symbolTable = environment.symbolTable.get(curScope);
        for (var i = symbolTable.size() - 1; i >= 0; i--) {
            var symbol = symbolTable.get(i).get(id);
            if (symbol != null) {
                return symbol;
            }
        }
        throw new RuntimeException("Variable " + id + " not found");
    }

    private String findVariableName(MiniJavaParser.ExpressionContext ctx) {
        assert ctx.primary() != null;
        if (ctx.primary().expression() != null) {
            return findVariableName(ctx.primary().expression());
        }
        assert ctx.primary().identifier().IDENTIFIER() != null;
        return ctx.primary().identifier().IDENTIFIER().getText();
    }

    private Map<String, MiniJavaObject> getCurSymbolTable() {
        return environment.symbolTable.get(curScope).getLast();
    }

    private void newSymbolTable() {
        environment.symbolTable.get(curScope).add(new HashMap<>());
    }

    private void removeSymbolTable() {
        environment.symbolTable.get(curScope).removeLast();
    }

    public void displayBytecodes(String filePath) {
        bytecodeGenerator.displayBytecodes(filePath);
    }

    public void displayEnvironment(String filePath) {
        environment.displayEnvironment(filePath);
    }

    @Override
    public MiniJavaObject visitCompilationUnit(MiniJavaParser.CompilationUnitContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public MiniJavaObject visitBlock(MiniJavaParser.BlockContext ctx) {
        newSymbolTable();
        var ret = visitChildren(ctx);
        removeSymbolTable();
        return ret;
    }

    @Override
    public MiniJavaObject visitBlockStatement(MiniJavaParser.BlockStatementContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public MiniJavaObject visitLocalVariableDeclaration(MiniJavaParser.LocalVariableDeclarationContext ctx) {
        var curSymbolTable = getCurSymbolTable();
        // primitiveType identifier
        if (ctx.getChildCount() == 2) {
            var defaultValue = visit(ctx.primitiveType());
            var identifier = ctx.identifier().getText();
            var ret = environment.newVariable(curScope, identifier, defaultValue);
            return ret;
        }
        // primitiveType identifier '=' expression
        else if (ctx.getChildCount() == 4) {
            var defaultValue = visit(ctx.primitiveType());
            var identifier = ctx.identifier().getText();
            visit(ctx.expression());
            var ret = environment.newVariable(curScope, identifier, defaultValue);
            if (ret.isGlobal()) {
                bytecodeGenerator.emitBytecode(BytecodeType.OP_SET_GLOBAL, ret.index, null);
                bytecodeGenerator.emitBytecode(BytecodeType.OP_POP, null, null);
                return ret;
            } else {
                bytecodeGenerator.emitBytecode(BytecodeType.OP_SET_LOCAL, ret.index, null);
                bytecodeGenerator.emitBytecode(BytecodeType.OP_POP, null, null);
                return ret;
            }
        }
        return null;
    }

    private MiniJavaObject visitIfStatement(MiniJavaParser.StatementContext ctx) {
        return null;
    }

    private MiniJavaObject visitWhileStatement(MiniJavaParser.StatementContext ctx) {
        return null;
    }

    private MiniJavaObject visitForStatement(MiniJavaParser.StatementContext ctx) {
        return null;
    }

    @Override
    public MiniJavaObject visitStatement(MiniJavaParser.StatementContext ctx) {

        if (ctx.block() != null) {
            return visitChildren(ctx);
        } else if (ctx.IF() != null) {
            return visitIfStatement(ctx);
        } else if (ctx.FOR() != null) {
            return visitForStatement(ctx);
        } else if (ctx.WHILE() != null) {
            return visitWhileStatement(ctx);
        } else if (ctx.CONTINUE() != null) {
            return null;
        } else if (ctx.BREAK() != null) {
            return null;
        } else if (ctx.expression() != null) {
            return visitChildren(ctx);
        } else if (ctx.SEMI() != null && ctx.getChildCount() == 1) {
            return visitChildren(ctx);
        } else {
            throw new RuntimeException("Unknown statement: " + ctx.getText());
        }
    }

    private MiniJavaObject visitShortCircuitExp(MiniJavaParser.ExpressionContext ctx) {
        return null;
    }

    private MiniJavaObject visitArithmeticExp(MiniJavaParser.ExpressionContext ctx, MiniJavaObject lhs, MiniJavaObject rhs) {
        return null;
    }

    private MiniJavaObject visitArithmeticAssign(MiniJavaParser.ExpressionContext ctx, MiniJavaObject lhs, MiniJavaObject rhs) {
        return null;
    }

    private MiniJavaObject visitBopExp(MiniJavaParser.ExpressionContext ctx) {
        return null;
    }

    private MiniJavaObject visitPostExp(MiniJavaParser.ExpressionContext ctx) {
        return null;
    }

    private MiniJavaObject visitPrefixExp(MiniJavaParser.ExpressionContext ctx) {
        return null;
    }

    @Override
    public MiniJavaObject visitExpression(MiniJavaParser.ExpressionContext ctx) {
        if (ctx.bop != null) {
            return visitBopExp(ctx);
        } else if (ctx.primary() != null) {
            return visit(ctx.primary());
        } else if (ctx.postfix != null) {
            return visitPostExp(ctx);
        } else if (ctx.prefix != null) {
            return visitPrefixExp(ctx);
        } else if (ctx.primitiveType() != null) {
            var type = visit(ctx.primitiveType()).type;
            var exp = visit(ctx.expression(0));
            exp.type = type;
            return exp;
        } else {
            throw new RuntimeException("Unknown expression: " + ctx.getText());
        }
    }

    @Override
    public MiniJavaObject visitPrimary(MiniJavaParser.PrimaryContext ctx) {
        if (ctx.getChildCount() == 3) {
            return visitExpression(ctx.expression());
        }
        return visitChildren(ctx);
    }

    @Override
    public MiniJavaObject visitLiteral(MiniJavaParser.LiteralContext ctx) {
        if (ctx.DECIMAL_LITERAL() != null) {
            var ret = new MiniJavaObject(MiniJavaType.INT, Integer.parseInt(ctx.getText()));
            ret = environment.newConstant(curScope, ret);
            bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, ret.index, null);
            return ret;
        } else if (ctx.STRING_LITERAL() != null) {
            var str = ctx.getText();
            var ret = new MiniJavaObject(MiniJavaType.STRING, str.substring(1, str.length() - 1));
            ret = environment.newConstant(curScope, ret);
            bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, ret.index, null);
            return ret;
        } else if (ctx.BOOL_LITERAL() != null) {
            var ret = new MiniJavaObject(MiniJavaType.BOOLEAN, "true".equals(ctx.getText()));
            ret = environment.newConstant(curScope, ret);
            bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, ret.index, null);
            return ret;
        } else if (ctx.CHAR_LITERAL() != null) {
            var ret = new MiniJavaObject(MiniJavaType.CHAR, ctx.getText().charAt(1));
            ret = environment.newConstant(curScope, ret);
            bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, ret.index, null);
            return ret;
        } else if (ctx.NULL_LITERAL() != null) {
            var ret = new MiniJavaObject(MiniJavaType.NULL, null);
            ret = environment.newConstant(curScope, ret);
            bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, ret.index, null);
            return ret;
        } else {
            throw new RuntimeException("Unknown literal: " + ctx.getText());
        }
    }

    @Override
    public MiniJavaObject visitIdentifier(MiniJavaParser.IdentifierContext ctx) {
        String identifier = ctx.IDENTIFIER().getText();
        var variable = findVariableByID(identifier);
        if (variable.isGlobal()) {
            bytecodeGenerator.emitBytecode(BytecodeType.OP_GET_GLOBAL, variable.index, null);
        } else {
            bytecodeGenerator.emitBytecode(BytecodeType.OP_GET_LOCAL, variable.index, null);
        }
        return variable;
    }

    @Override
    public MiniJavaObject visitPrimitiveType(MiniJavaParser.PrimitiveTypeContext ctx) {
        String type_str = ctx.getText();
        MiniJavaType type;
        Object value = null;
        if ("int".equals(type_str)) {
            type = MiniJavaType.INT;
            value = 0;
        } else if ("char".equals(type_str)) {
            type = MiniJavaType.CHAR;
            value = 0;
        } else if ("boolean".equals(type_str)) {
            type = MiniJavaType.BOOLEAN;
            value = false;
        } else if ("string".equals(type_str)) {
            type = MiniJavaType.STRING;
            value = "";
        } else {
            throw new RuntimeException("Unknown primitive type: " + type_str);
        }
        return new MiniJavaObject(type, value);
    }
}
