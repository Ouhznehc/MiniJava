import java.util.HashMap;

public class TreeWalkVisitor extends MiniJavaParserBaseVisitor<MiniJavaObject> {

    private final Environment environment = new Environment();
    private final BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

    private String curScope = "0_global";
    private Integer curLabel = 0;

    private void getVariable(MiniJavaObject variable) {
        if (variable.isGlobal()) {
            bytecodeGenerator.emitBytecode(BytecodeType.OP_GET_GLOBAL, variable.index, null);
        } else {
            bytecodeGenerator.emitBytecode(BytecodeType.OP_GET_LOCAL, variable.index, null);
        }
    }

    private void setVariable(MiniJavaObject variable) {
        if (variable.isGlobal()) {
            bytecodeGenerator.emitBytecode(BytecodeType.OP_SET_GLOBAL, variable.index, null);
            bytecodeGenerator.emitBytecode(BytecodeType.OP_POP, null, null);
        } else {
            bytecodeGenerator.emitBytecode(BytecodeType.OP_SET_LOCAL, variable.index, null);
            bytecodeGenerator.emitBytecode(BytecodeType.OP_POP, null, null);
        }
    }
    
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

    private boolean isConditionExp(MiniJavaParser.ExpressionContext ctx) {
        if (ctx.bop != null) {
            if (ctx.bop.getType() == MiniJavaParser.AND 
                || ctx.bop.getType() == MiniJavaParser.OR
                || ctx.bop.getType() == MiniJavaParser.EQUAL
                || ctx.bop.getType() == MiniJavaParser.NOTEQUAL
                || ctx.bop.getType() == MiniJavaParser.LT
                || ctx.bop.getType() == MiniJavaParser.LE
                || ctx.bop.getType() == MiniJavaParser.GT
                || ctx.bop.getType() == MiniJavaParser.GE
                || ctx.bop.getType() == MiniJavaParser.QUESTION){
                return true;
            }
        }
        if (ctx.prefix != null) {
            if (ctx.prefix.getType() == MiniJavaParser.BANG) {
                return true;
            }
        }
        return false;
    }

    private boolean isArithmeticAssignExp(int type) {
        return type == MiniJavaParser.ASSIGN
            || type == MiniJavaParser.ADD_ASSIGN
            || type == MiniJavaParser.SUB_ASSIGN
            || type == MiniJavaParser.MUL_ASSIGN
            || type == MiniJavaParser.DIV_ASSIGN
            || type == MiniJavaParser.MOD_ASSIGN
            || type == MiniJavaParser.LSHIFT_ASSIGN
            || type == MiniJavaParser.RSHIFT_ASSIGN
            || type == MiniJavaParser.URSHIFT_ASSIGN
            || type == MiniJavaParser.AND_ASSIGN
            || type == MiniJavaParser.OR_ASSIGN
            || type == MiniJavaParser.XOR_ASSIGN;
    }

    private boolean isArithmeticExp(int type) {
        return type == MiniJavaParser.ADD
            || type == MiniJavaParser.SUB
            || type == MiniJavaParser.MUL
            || type == MiniJavaParser.DIV
            || type == MiniJavaParser.MOD
            || type == MiniJavaParser.LSHIFT
            || type == MiniJavaParser.RSHIFT
            || type == MiniJavaParser.URSHIFT
            || type == MiniJavaParser.BITAND
            || type == MiniJavaParser.BITOR
            || type == MiniJavaParser.CARET;
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
            setVariable(ret);
            return ret;
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


    private MiniJavaObject visitArithmeticExp(MiniJavaParser.ExpressionContext ctx) {
        // First push rhs to stack, then lhs
        var rhs = visit(ctx.expression(1));
        var lhs = visit(ctx.expression(0));
        var type = MiniJavaType.maxType(lhs.type, rhs.type);
        switch (ctx.bop.getType()) {
            case MiniJavaParser.ADD:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_ADD, null, null);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.SUB:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_SUB, null, null);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.MUL:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_MUL, null, null);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.DIV:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_DIV, null, null);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.MOD:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_MOD, null, null);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.LSHIFT:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_LSHIFT, null, null);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.RSHIFT:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_RSHIFT, null, null);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.URSHIFT:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_URSHIFT, null, null);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.BITAND:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_BIT_AND, null, null);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.BITOR:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_BIT_OR, null, null);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.CARET:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_BIT_XOR, null, null);
                return new MiniJavaObject(type, null);
            default:
                return null;
        }
    }

    private MiniJavaObject visitArithmeticAssign(MiniJavaParser.ExpressionContext ctx) {
        var id = findVariableName(ctx.expression(0));
        var variable = findVariableByID(id);
        // For assignment, we only need to push rhs to stack
        if (ctx.bop.getType() == MiniJavaParser.ASSIGN) {
            var rhs = visit(ctx.expression(1));
            setVariable(variable);
            return new MiniJavaObject(rhs.type, null);
        }
        // First push rhs to stack, then lhs
        var rhs = visit(ctx.expression(1));
        var lhs = visit(ctx.expression(0));
        var type = MiniJavaType.maxType(lhs.type, rhs.type);
        switch (ctx.bop.getType()) {
            case MiniJavaParser.ADD_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_ADD, null, null);
                setVariable(variable);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.SUB_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_SUB, null, null);
                setVariable(variable);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.MUL_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_MUL, null, null);
                setVariable(variable);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.DIV_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_DIV, null, null);
                setVariable(variable);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.MOD_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_MOD, null, null);
                setVariable(variable);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.LSHIFT_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_LSHIFT, null, null);
                setVariable(variable);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.RSHIFT_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_RSHIFT, null, null);
                setVariable(variable);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.URSHIFT_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_URSHIFT, null, null);
                setVariable(variable);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.AND_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_BIT_AND, null, null);
                setVariable(variable);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.OR_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_BIT_OR, null, null);
                setVariable(variable);
                return new MiniJavaObject(type, null);
            case MiniJavaParser.XOR_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_BIT_XOR, null, null);
                setVariable(variable);
                return new MiniJavaObject(type, null);
            default:
                return null;
        }
    }

    private MiniJavaObject visitBopExp(MiniJavaParser.ExpressionContext ctx) {
        if (isArithmeticExp(ctx.bop.getType())) {
            return visitArithmeticExp(ctx);
        } else if (isArithmeticAssignExp(ctx.bop.getType())) {
            return visitArithmeticAssign(ctx);
        } else {
            throw new RuntimeException("Unknown binary operation: " + ctx.bop.getText());
        }
    }

    private MiniJavaObject visitPostExp(MiniJavaParser.ExpressionContext ctx) {
        var id = findVariableName(ctx.expression(0));
        var variable = findVariableByID(id);
        var exp = visit(ctx.expression(0));
        if (ctx.postfix.getType() == MiniJavaParser.INC) {
            bytecodeGenerator.emitBytecode(BytecodeType.OP_POST_INC, null, null);
            setVariable(variable);
        } else if (ctx.postfix.getType() == MiniJavaParser.DEC) {
            bytecodeGenerator.emitBytecode(BytecodeType.OP_POST_DEC, null, null);
            setVariable(variable);
        } else {
            throw new RuntimeException("Unknown postfix expression: " + ctx.postfix.getText());
        }
        return exp;
    }

    private MiniJavaObject visitPrefixExp(MiniJavaParser.ExpressionContext ctx) {
        var exp = visit(ctx.expression(0));
        if (ctx.prefix.getType() == MiniJavaParser.INC) {
            bytecodeGenerator.emitBytecode(BytecodeType.OP_PRE_INC, null, null);
            setVariable(exp);
            return exp;
        } else if (ctx.prefix.getType() == MiniJavaParser.DEC) {
            bytecodeGenerator.emitBytecode(BytecodeType.OP_PRE_DEC, null, null);
            setVariable(exp);
            return exp;
        } else if (ctx.prefix.getType() == MiniJavaParser.TILDE) {
            bytecodeGenerator.emitBytecode(BytecodeType.OP_BIT_NOT, null, null);
            return exp;
        } else if (ctx.prefix.getType() == MiniJavaParser.ADD) {
            return exp;
        } else if (ctx.prefix.getType() == MiniJavaParser.SUB) {
            bytecodeGenerator.emitBytecode(BytecodeType.OP_NEG, null, null);
            return exp;
        } else {
            throw new RuntimeException("Unknown prefix expression: " + ctx.prefix.getText());
        }
    }

    private MiniJavaObject visitConditionExp(MiniJavaParser.ExpressionContext ctx, Integer true_label, Integer false_label) {
        //TODO: implement this
        return null;
    }

    @Override
    public MiniJavaObject visitExpression(MiniJavaParser.ExpressionContext ctx) {
        if (isConditionExp(ctx)) {
            Integer true_label = curLabel++;
            Integer false_label = curLabel++;
            visitConditionExp(ctx, true_label, false_label);
            return null;
        }else if (ctx.bop != null) {
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
        getVariable(variable);
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
