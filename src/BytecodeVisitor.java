import java.util.ArrayList;
import java.util.Stack;

public class BytecodeVisitor extends MiniJavaParserBaseVisitor<Void> {

    private final Environment environment;
    private final BytecodeGenerator bytecodeGenerator;
    private final SemanticsVisitor semanticsVisitor;

    public BytecodeVisitor(BytecodeGenerator bytecodeGenerator, Environment environment, SemanticsVisitor semanticsVisitor) {
        this.bytecodeGenerator = bytecodeGenerator;
        this.environment = environment;
        this.semanticsVisitor = semanticsVisitor;
    }

    private Stack<Integer> breakStack = new Stack<>();
    private Stack<Integer> continueStack = new Stack<>();

    private boolean isConditionExp(MiniJavaParser.ExpressionContext ctx) {
        if (ctx.bop != null) {
            if (ctx.bop.getType() == MiniJavaParser.AND
                    || ctx.bop.getType() == MiniJavaParser.OR
                    || ctx.bop.getType() == MiniJavaParser.EQUAL
                    || ctx.bop.getType() == MiniJavaParser.NOTEQUAL
                    || ctx.bop.getType() == MiniJavaParser.LT
                    || ctx.bop.getType() == MiniJavaParser.LE
                    || ctx.bop.getType() == MiniJavaParser.GT
                    || ctx.bop.getType() == MiniJavaParser.GE) {
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

    private boolean isQuestionExp(MiniJavaParser.ExpressionContext ctx) {
        return ctx.bop != null && ctx.bop.getType() == MiniJavaParser.QUESTION;
    }

    private MiniJavaParser.ExpressionContext getLeftValueExp(MiniJavaParser.ExpressionContext ctx) {
        if (ctx.primary() != null) {
            if (ctx.primary().expression() != null) return getLeftValueExp(ctx.primary().expression());
            if (ctx.primary().identifier() != null) return ctx;
        }
        if (ctx.LBRACK() != null) return ctx;
        throw new RuntimeException("Unknown left value expression: " + ctx.getText());
    }

    private MiniJavaObject prepareLeftValue(MiniJavaParser.ExpressionContext ctx) {
        // Simple vaiable
        if (ctx.primary() != null) {
            var id = ctx.primary().identifier().getText();
            var variable = environment.findVariable(id);
            return variable;
        }
        // Array 
        else if (ctx.LBRACK() != null) {
            visit(ctx.expression(0));
            visit(ctx.expression(1));
            return null;
        }
        else {
            throw new RuntimeException("Unknown left value expression: " + ctx.getText());
        }
    }

    @Override
    public Void visitCompilationUnit(MiniJavaParser.CompilationUnitContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Void visitMethodDeclaration(MiniJavaParser.MethodDeclarationContext ctx) {
        var methodName = ctx.identifier().getText();
        var paramTypes = new ArrayList<MiniJavaType>();
        var params = new ArrayList<MiniJavaObject>();
        if (ctx.formalParameters().formalParameterList() != null) {
            for (var param : ctx.formalParameters().formalParameterList().formalParameter()) {
                var type = param.typeType().getText();
                var id = param.identifier().getText();
                paramTypes.add(new MiniJavaType(type));
                params.add(new MiniJavaObject(type, id));
            }
        }
        var returnType = ctx.VOID() != null ? new MiniJavaType("void") : new MiniJavaType(ctx.typeType().getText());
        var methodSig = new MethodSignature(methodName, paramTypes, returnType);
        var methodMangle = methodSig.mangle();
        environment.newMethod(methodMangle, params);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_METHOD, methodMangle);
        visit(ctx.methodBody);
        return null;
    }

    @Override
    public Void visitBlock(MiniJavaParser.BlockContext ctx) {
        environment.newSymbolTable();
        visitChildren(ctx);
        environment.removeSymbolTable();
        return null;
    }

    @Override
    public Void visitBlockStatement(MiniJavaParser.BlockStatementContext ctx) {
        if (ctx.localVariableDeclaration() != null) {
            visit(ctx.localVariableDeclaration());
            bytecodeGenerator.emitBytecode(BytecodeType.OP_POP);
            return null;
        } else {
            return visit(ctx.statement());
        }
    }

    @Override
    public Void visitArrayInitializer(MiniJavaParser.ArrayInitializerContext ctx) {
        var initializers = ctx.variableInitializer();
        if (initializers == null) {
            var arraySize = environment.newConstant("int", 0);
            bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, arraySize.index);
            bytecodeGenerator.emitBytecode(BytecodeType.OP_NEW_ARRAY);
            return null;
        }
        var size = initializers.size();
        // Create a new array of the given size
        var arraySize = environment.newConstant("int", size);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, arraySize.index);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_NEW_ARRAY);
        for (var i = 0; i < size; i++) {
            bytecodeGenerator.emitBytecode(BytecodeType.OP_DUP); 
            var index = environment.newConstant("int", i);
            bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, index.index);
            visit(initializers.get(i));
            bytecodeGenerator.emitBytecode(BytecodeType.OP_SET_INDEX);
            bytecodeGenerator.emitBytecode(BytecodeType.OP_POP);
        }
        return null;
    }

    @Override
    public Void visitVariableInitializer(MiniJavaParser.VariableInitializerContext ctx) {
        if (ctx.expression() != null) {
            return visit(ctx.expression());
        } else if (ctx.arrayInitializer() != null) {
            return visit(ctx.arrayInitializer());
        } else {
            throw new RuntimeException("Unknown variable initializer: " + ctx.getText());
        }
    }

    @Override
    public Void visitLocalVariableDeclaration(MiniJavaParser.LocalVariableDeclarationContext ctx) {
        // typeType variableDeclarator
        if (ctx.VAR() == null) {
            var defaultValue = new MiniJavaType(ctx.typeType().getText());
            var declarator = ctx.variableDeclarator();
            var identifier = declarator.identifier().getText();
            // If the variable is not initialized, we need to set a default value
            // to the variable in the symbol table
            if (declarator.variableInitializer() == null) {
                environment.newVariable(defaultValue, identifier);
            } else {
                visit(declarator.variableInitializer());
                var type = semanticsVisitor.getType(declarator.variableInitializer());
                var variable = environment.newVariable(type, identifier);
                bytecodeGenerator.setVariable(variable);
            }
            return null;
        }
        // VAR identifier '=' expression
        else {
            var identifier = ctx.identifier().getText();
            var type = semanticsVisitor.getType(ctx.expression());
            visit(ctx.expression());
            var variable = environment.newVariable(type, identifier);
            bytecodeGenerator.setVariable(variable);
            return null;
        }
    }

    private Void visitIfStatement(MiniJavaParser.StatementContext ctx) {
        if (ctx.ELSE() == null) {
            Integer true_label = environment.newLabel();
            Integer end_label = environment.newLabel();
            visitConditionExp(ctx.parExpression().expression(), true_label, end_label);
            bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, true_label);
            visit(ctx.statement(0));
            bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, end_label);
        } else {
            Integer true_label = environment.newLabel();
            Integer false_label = environment.newLabel();
            Integer end_label = environment.newLabel();
            visitConditionExp(ctx.parExpression().expression(), true_label, false_label);
            bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, true_label);
            visit(ctx.statement(0));
            bytecodeGenerator.emitBytecode(BytecodeType.OP_JUMP, end_label);
            bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, false_label);
            visit(ctx.statement(1));
            bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, end_label);
        }
        return null;
    }

    private Void visitWhileStatement(MiniJavaParser.StatementContext ctx) {
        // while ( parExpression ) statement

        // Generate labels for the loop:
        Integer start_label = environment.newLabel(); // Loop condition check label
        Integer true_label = environment.newLabel(); // Label for entering the loop body
        Integer end_label = environment.newLabel(); // Label for loop exit

        // Push the current loop's labels onto the stacks
        breakStack.push(end_label);
        continueStack.push(start_label);

        // Emit label for the start of the loop (condition evaluation)
        bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, start_label);
        // Evaluate the condition: if true, jump to true_label; if false, jump to
        // end_label.
        visitConditionExp(ctx.parExpression().expression(), true_label, end_label);
        // Emit label for the loop body (true branch)
        bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, true_label);
        // Process the loop body
        visit(ctx.statement(0));
        // After executing the body, jump back to the condition evaluation
        bytecodeGenerator.emitBytecode(BytecodeType.OP_JUMP, start_label);
        // Emit the loop exit label
        bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, end_label);

        // Pop loop labels off the stacks
        breakStack.pop();
        continueStack.pop();

        return null;
    }

    private Void visitForStatement(MiniJavaParser.StatementContext ctx) {
        // for ( forControl ) statement

        environment.newSymbolTable();

        // Process initialization (if present)
        var forInit = ctx.forControl().forInit();
        if (forInit != null)
            visit(forInit);

        // Generate labels:
        Integer start_label = environment.newLabel(); // Condition check label (also used for continue)
        Integer true_label = environment.newLabel(); // Label for entering loop body
        Integer end_label = environment.newLabel(); // Exit label for break

        // Push the loop labels
        breakStack.push(end_label);
        continueStack.push(start_label);

        // Emit label for condition evaluation
        bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, start_label);

        // Evaluate the loop condition if present; if not, always true
        if (ctx.forControl().expression() != null) {
            visitConditionExp(ctx.forControl().expression(), true_label, end_label);
        } else {
            // No condition means always jump to the body
            bytecodeGenerator.emitBytecode(BytecodeType.OP_JUMP, true_label);
        }

        // Emit label for loop body
        bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, true_label);
        // Process the loop body
        visit(ctx.statement(0));

        // Process update expression(s) if present (forUpdate is an expressionList)
        if (ctx.forControl().forUpdate != null) {
            visit(ctx.forControl().forUpdate);
        }
        // Jump back to condition check
        bytecodeGenerator.emitBytecode(BytecodeType.OP_JUMP, start_label);
        // Emit loop exit label
        bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, end_label);

        // Pop the loop labels off the stacks
        breakStack.pop();
        continueStack.pop();

        environment.removeSymbolTable();

        return null;
    }

    private Void visitContinueStatement(MiniJavaParser.StatementContext ctx) {
        if (continueStack.isEmpty())
            throw new RuntimeException("Continue statement not within a loop");
        // Emit a jump to the current loop's continue target (condition check)
        int continue_target = continueStack.peek();
        bytecodeGenerator.emitBytecode(BytecodeType.OP_JUMP, continue_target);
        return null;
    }

    private Void visitBreakStatement(MiniJavaParser.StatementContext ctx) {
        if (breakStack.isEmpty())
            throw new RuntimeException("Break statement not within a loop");
        // Emit a jump to the current loop's break target (loop exit)
        int break_target = breakStack.peek();
        bytecodeGenerator.emitBytecode(BytecodeType.OP_JUMP, break_target);
        return null;
    }

    private Void visitReturnStatement(MiniJavaParser.StatementContext ctx) {
        if (ctx.expression() != null) {
            visit(ctx.expression());
            bytecodeGenerator.emitBytecode(BytecodeType.OP_RETURN);
        } else {
            bytecodeGenerator.emitBytecode(BytecodeType.OP_RETURN);
        }
        return null;
    }

    @Override
    public Void visitStatement(MiniJavaParser.StatementContext ctx) {
        if (ctx.block() != null) {
            return visitChildren(ctx);
        } else if (ctx.IF() != null) {
            return visitIfStatement(ctx);
        } else if (ctx.FOR() != null) {
            return visitForStatement(ctx);
        } else if (ctx.WHILE() != null) {
            return visitWhileStatement(ctx);
        } else if (ctx.CONTINUE() != null) {
            return visitContinueStatement(ctx);
        } else if (ctx.BREAK() != null) {
            return visitBreakStatement(ctx);
        } else if (ctx.RETURN() != null) {
            return visitReturnStatement(ctx);
        } else if (ctx.expression() != null) {
            visit(ctx.expression());
            bytecodeGenerator.emitBytecode(BytecodeType.OP_POP);
            return null;
        } else if (ctx.SEMI() != null && ctx.getChildCount() == 1) {
            return visitChildren(ctx);
        } else {
            throw new RuntimeException("Unknown statement: " + ctx.getText());
        }
    }

    private Void visitArithmeticExp(MiniJavaParser.ExpressionContext ctx) {
        // First push rhs to stack, then rhs
        visit(ctx.expression(0));
        visit(ctx.expression(1));
        switch (ctx.bop.getType()) {
            case MiniJavaParser.ADD:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_ADD);
                return null;
            case MiniJavaParser.SUB:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_SUB);
                return null;
            case MiniJavaParser.MUL:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_MUL);
                return null;
            case MiniJavaParser.DIV:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_DIV);
                return null;
            case MiniJavaParser.MOD:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_MOD);
                return null;
            case MiniJavaParser.LSHIFT:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_LSHIFT);
                return null;
            case MiniJavaParser.RSHIFT:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_RSHIFT);
                return null;
            case MiniJavaParser.URSHIFT:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_URSHIFT);
                return null;
            case MiniJavaParser.BITAND:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_BIT_AND);
                return null;
            case MiniJavaParser.BITOR:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_BIT_OR);
                return null;
            case MiniJavaParser.CARET:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_BIT_XOR);
                return null;
            default:
                return null;
        }
    }

    private Void visitArithmeticAssign(MiniJavaParser.ExpressionContext ctx) {
        var leftExp = getLeftValueExp(ctx.expression(0));
        var object = prepareLeftValue(leftExp);
        if (ctx.bop.getType() == MiniJavaParser.ASSIGN) {
            var rhs = visit(ctx.expression(1));
            bytecodeGenerator.setVariable(object);
            return null;
        }
        // lhs is a variable, we need to load it first
        visit(ctx.expression(0));
        visit(ctx.expression(1));
        switch (ctx.bop.getType()) {
            case MiniJavaParser.ADD_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_ADD);
                bytecodeGenerator.setVariable(object);
                return null;
            case MiniJavaParser.SUB_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_SUB);
                bytecodeGenerator.setVariable(object);
                return null;
            case MiniJavaParser.MUL_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_MUL);
                bytecodeGenerator.setVariable(object);
                return null;
            case MiniJavaParser.DIV_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_DIV);
                bytecodeGenerator.setVariable(object);
                return null;
            case MiniJavaParser.MOD_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_MOD);
                bytecodeGenerator.setVariable(object);
                return null;
            case MiniJavaParser.LSHIFT_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_LSHIFT);
                bytecodeGenerator.setVariable(object);
                return null;
            case MiniJavaParser.RSHIFT_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_RSHIFT);
                bytecodeGenerator.setVariable(object);
                return null;
            case MiniJavaParser.URSHIFT_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_URSHIFT);
                bytecodeGenerator.setVariable(object);
                return null;
            case MiniJavaParser.AND_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_BIT_AND);
                bytecodeGenerator.setVariable(object);
                return null;
            case MiniJavaParser.OR_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_BIT_OR);
                bytecodeGenerator.setVariable(object);
                return null;
            case MiniJavaParser.XOR_ASSIGN:
                bytecodeGenerator.emitBytecode(BytecodeType.OP_BIT_XOR);
                bytecodeGenerator.setVariable(object);
                return null;
            default:
                return null;
        }
    }

    private Void visitBopExp(MiniJavaParser.ExpressionContext ctx) {
        if (isArithmeticExp(ctx.bop.getType())) {
            return visitArithmeticExp(ctx);
        } else if (isArithmeticAssignExp(ctx.bop.getType())) {
            return visitArithmeticAssign(ctx);
        } else {
            throw new RuntimeException("Unknown binary operation: " + ctx.bop.getText());
        }
    }

    private Void visitPostExp(MiniJavaParser.ExpressionContext ctx) {
        visit(ctx.expression(0));
        var leftExp = getLeftValueExp(ctx.expression(0));
        var object = prepareLeftValue(leftExp);
        visit(ctx.expression(0));
        if (ctx.postfix.getType() == MiniJavaParser.INC) {
            bytecodeGenerator.emitBytecode(BytecodeType.OP_INC);
            bytecodeGenerator.setVariable(object);
            return null;
        } else if (ctx.postfix.getType() == MiniJavaParser.DEC) {
            bytecodeGenerator.emitBytecode(BytecodeType.OP_DEC);
            bytecodeGenerator.setVariable(object);
            return null;
        }
        return null;
    }

    private Void visitPrefixExp(MiniJavaParser.ExpressionContext ctx) {
        if (ctx.prefix.getType() == MiniJavaParser.INC) {
            var leftExp = getLeftValueExp(ctx.expression(0));
            var object = prepareLeftValue(leftExp);
            visit(ctx.expression(0));
            bytecodeGenerator.emitBytecode(BytecodeType.OP_INC);
            bytecodeGenerator.setVariable(object);
            return null;
        } else if (ctx.prefix.getType() == MiniJavaParser.DEC) {
            var leftExp = getLeftValueExp(ctx.expression(0));
            var object = prepareLeftValue(leftExp);
            visit(ctx.expression(0));
            bytecodeGenerator.emitBytecode(BytecodeType.OP_DEC);
            bytecodeGenerator.setVariable(object);
            return null;
        } else if (ctx.prefix.getType() == MiniJavaParser.TILDE) {
            visit(ctx.expression(0));
            bytecodeGenerator.emitBytecode(BytecodeType.OP_BIT_NOT);
            return null;
        } else if (ctx.prefix.getType() == MiniJavaParser.ADD) {
            visit(ctx.expression(0));
            return null;
        } else if (ctx.prefix.getType() == MiniJavaParser.SUB) {
            visit(ctx.expression(0));
            bytecodeGenerator.emitBytecode(BytecodeType.OP_NEG);
            return null;
        } else {
            throw new RuntimeException("Unknown prefix expression: " + ctx.prefix.getText());
        }
    }

    private void visitConditionExp(MiniJavaParser.ExpressionContext ctx, Integer true_label, Integer false_label) {
        if (ctx.bop != null) {
            if (ctx.bop.getType() == MiniJavaParser.AND) {
                Integer label = environment.newLabel();
                visitConditionExp(ctx.expression(0), label, false_label);
                bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, label);
                visitConditionExp(ctx.expression(1), true_label, false_label);
            } else if (ctx.bop.getType() == MiniJavaParser.OR) {
                Integer label = environment.newLabel();
                visitConditionExp(ctx.expression(0), true_label, label);
                bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, label);
                visitConditionExp(ctx.expression(1), true_label, false_label);
            } else {
                // Exp -> Exp RELOP Exp
                // First visit lhs, then rhs
                visit(ctx.expression(0));
                visit(ctx.expression(1));
                switch (ctx.bop.getType()) {
                    case MiniJavaParser.EQUAL:
                        bytecodeGenerator.emitBytecode(BytecodeType.OP_EQ);
                        break;
                    case MiniJavaParser.NOTEQUAL:
                        bytecodeGenerator.emitBytecode(BytecodeType.OP_NEQ);
                        break;
                    case MiniJavaParser.LT:
                        bytecodeGenerator.emitBytecode(BytecodeType.OP_LT);
                        break;
                    case MiniJavaParser.LE:
                        bytecodeGenerator.emitBytecode(BytecodeType.OP_LE);
                        break;
                    case MiniJavaParser.GT:
                        bytecodeGenerator.emitBytecode(BytecodeType.OP_GT);
                        break;
                    case MiniJavaParser.GE:
                        bytecodeGenerator.emitBytecode(BytecodeType.OP_GE);
                        break;
                }
                bytecodeGenerator.emitBytecode(BytecodeType.OP_JUMP_IF_TRUE, true_label);
                bytecodeGenerator.emitBytecode(BytecodeType.OP_JUMP, false_label);
            }
        } else if (ctx.prefix != null) {
            // Exp -> !Exp
            visitConditionExp(ctx.expression(0), false_label, true_label);
        } else {
            throw new RuntimeException("Unknown condition expression: " + ctx.getText());
        }
    }

    private Void visitConditionalExp(MiniJavaParser.ExpressionContext ctx) {
        Integer true_label = environment.newLabel();
        Integer false_label = environment.newLabel();
        bytecodeGenerator.emitBytecode(BytecodeType.OP_FALSE);
        visitConditionExp(ctx, true_label, false_label);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, true_label);
        // Pop out the False value
        bytecodeGenerator.emitBytecode(BytecodeType.OP_POP);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_TRUE);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, false_label);
        return null;
    }

    private Void visitQuestionExp(MiniJavaParser.ExpressionContext ctx) {
        Integer false_label = environment.newLabel();
        Integer end_label = environment.newLabel();
        visitExpression(ctx.expression(0));
        bytecodeGenerator.emitBytecode(BytecodeType.OP_JUMP_IF_FALSE, false_label);
        // True expression
        visitExpression(ctx.expression(1));
        bytecodeGenerator.emitBytecode(BytecodeType.OP_JUMP, end_label);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, false_label);
        // False expression
        visitExpression(ctx.expression(2));
        bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, end_label);
        return null;
    }

    private Void visitArrayIndex(MiniJavaParser.ExpressionContext ctx) {
        visit(ctx.expression(0));
        visit(ctx.expression(1));
        bytecodeGenerator.emitBytecode(BytecodeType.OP_GET_INDEX);
        return null;
    }

    private Void visitArrayCreator(MiniJavaParser.CreatorContext ctx, int curDim) {
        var rest = ctx.arrayCreatorRest();
        var expList = rest.expression();

        int providedDims = expList.size();
        int totalDims = rest.LBRACK().size();

        if (curDim >= providedDims) {
            if (providedDims == totalDims) {
                bytecodeGenerator.emitBytecode(BytecodeType.OP_NIL);
                return null;
            } else {
                var type = ctx.createdName().primitiveType().getText();
                var primitive = new MiniJavaType(type);
                if (primitive.isInt()) {
                    var defaultInt = environment.newConstant("int", 0);
                    bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, defaultInt.index);
                    return null;
                } else if (primitive.isBoolean()) {
                    var defaultBool = environment.newConstant("boolean", false);
                    bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, defaultBool.index);
                    return null;
                } else if (primitive.isChar()) {
                    var defaultChar = environment.newConstant("char", '\0');
                    bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, defaultChar.index);
                    return null;
                } else if (primitive.isString()) {
                    var defaultString = environment.newConstant("string", "");
                    bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, defaultString.index);
                    return null;
                }
                throw new RuntimeException("Unknown primitive type: " + type);
            }
        }

        // int[a + b][c][]
        visit(expList.get(curDim));
        // sizeVar = a + b, stack: { sizeVar }
        var sizeVar = environment.newTemp();
        bytecodeGenerator.emitBytecode(BytecodeType.OP_SET_LOCAL, sizeVar.index);
        // stack: {}
        bytecodeGenerator.emitBytecode(BytecodeType.OP_NEW_ARRAY);
        var arrayVar = environment.newTemp();
        bytecodeGenerator.emitBytecode(BytecodeType.OP_SET_LOCAL, arrayVar.index);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_POP);

        // stack: {}
        var indexVar = environment.newTemp();
        var defaultValue = environment.newConstant("int", 0);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, defaultValue.index);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_SET_LOCAL, indexVar.index);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_POP);

        var loopStart = environment.newLabel();
        var loopEnd = environment.newLabel();

        bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, loopStart);
        // stack: { indexVar , sizeVar }
        bytecodeGenerator.emitBytecode(BytecodeType.OP_GET_LOCAL, indexVar.index);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_GET_LOCAL, sizeVar.index);
        // stack: { indexVar >= sizeVar }
        bytecodeGenerator.emitBytecode(BytecodeType.OP_GE);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_JUMP_IF_TRUE, loopEnd);
        // stack: { int[], indexVar }
        bytecodeGenerator.emitBytecode(BytecodeType.OP_GET_LOCAL, arrayVar.index);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_GET_LOCAL, indexVar.index);
        // stack: { int[], indexVar, subArray}
        visitArrayCreator(ctx, curDim + 1);
        // stack: {}
        bytecodeGenerator.emitBytecode(BytecodeType.OP_SET_INDEX);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_POP);
        // stack: { indexVar }
        bytecodeGenerator.emitBytecode(BytecodeType.OP_GET_LOCAL, indexVar.index);
        // stack: { indexVar + 1 }
        bytecodeGenerator.emitBytecode(BytecodeType.OP_INC);
        // stack: {}
        bytecodeGenerator.emitBytecode(BytecodeType.OP_SET_LOCAL, indexVar.index);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_POP);

        bytecodeGenerator.emitBytecode(BytecodeType.OP_JUMP, loopStart);
        bytecodeGenerator.emitBytecode(BytecodeType.OP_LABEL, loopEnd);

        return null;
    }

    @Override
    public Void visitCreator(MiniJavaParser.CreatorContext ctx) {
        var rest = ctx.arrayCreatorRest();
        if (rest.arrayInitializer() != null) {
            return visit(rest.arrayInitializer());
        }
        else return visitArrayCreator(ctx, 0);
    }


    @Override
    public Void visitMethodCall(MiniJavaParser.MethodCallContext ctx) {
        var argumentTypes = new ArrayList<MiniJavaType>();
        if (ctx.arguments().expressionList() != null) {
            for (var exp : ctx.arguments().expressionList().expression()) {
                visit(exp);
                var arg = semanticsVisitor.getType(exp);
                argumentTypes.add(arg);
            }
        }

        var methodName = environment.newConstant("string", semanticsVisitor.getMangledMethod(ctx));
        bytecodeGenerator.emitBytecode(BytecodeType.OP_CALL, methodName.index, argumentTypes.size());
        return null;
    }

    @Override
    public Void visitExpression(MiniJavaParser.ExpressionContext ctx) {
        if (ctx.LBRACK() != null) {
           return visitArrayIndex(ctx);
        } else if (ctx.creator() != null) { 
            return visit(ctx.creator());
        } else if (ctx.methodCall() != null) {
            return visit(ctx.methodCall());
        } else if (isConditionExp(ctx)) {
           return visitConditionalExp(ctx);
        } else if (isQuestionExp(ctx)) {
            return visitQuestionExp(ctx);
        } else if (ctx.bop != null) {
            return visitBopExp(ctx);
        } else if (ctx.primary() != null) {
            return visit(ctx.primary());
        } else if (ctx.postfix != null) {
            return visitPostExp(ctx);
        } else if (ctx.prefix != null) {
            return visitPrefixExp(ctx);
        } else if (ctx.typeType() != null) {
            visit(ctx.expression(0));
            return null;
        } else {
            throw new RuntimeException("Unknown expression: " + ctx.getText());
        }
    }

    @Override
    public Void visitPrimary(MiniJavaParser.PrimaryContext ctx) {
        if (ctx.getChildCount() == 3) {
            return visitExpression(ctx.expression());
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitLiteral(MiniJavaParser.LiteralContext ctx) {
        if (ctx.DECIMAL_LITERAL() != null) {
            var ret = environment.newConstant("int", Integer.parseInt(ctx.getText()));
            bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, ret.index);
            return null;
        } else if (ctx.STRING_LITERAL() != null) {
            var str = ctx.getText();
            var ret = environment.newConstant("string", str.substring(1, str.length() - 1));
            bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, ret.index);
            return null;
        } else if (ctx.BOOL_LITERAL() != null) {
            var ret = environment.newConstant("boolean", "true".equals(ctx.getText()));
            bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, ret.index);
            return null;
        } else if (ctx.CHAR_LITERAL() != null) {
            var ret = environment.newConstant("char", ctx.getText().charAt(1));
            bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, ret.index);
            return null;
        } else if (ctx.NULL_LITERAL() != null) {
            var ret = environment.newConstant("null", null);
            bytecodeGenerator.emitBytecode(BytecodeType.OP_CONSTANT, ret.index);
            return null;
        } else {
            throw new RuntimeException("Unknown literal: " + ctx.getText());
        }
    }

    @Override
    public Void visitIdentifier(MiniJavaParser.IdentifierContext ctx) {
        String identifier = ctx.IDENTIFIER().getText();
        var variable = environment.findVariable(identifier);
        bytecodeGenerator.getVariable(variable);
        return null;
    }
}
