import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.ParserRuleContext;

/***
    * This class is responsible for semantic analysis of the MiniJava program.
    * It extends the MiniJavaParserBaseVisitor class to visit each node in the parse tree.
    * The visitor checks for semantic errors and collects type information.

    * The visitor uses `symbolTable` to keep track of the type of a variable.
    * The symbol table is a stack of maps, where each map represents a scope/block.
    * When enter a new method, we clear the WHOLE symbol table.

    * The visitor also uses `typeMap` to store the type of each node in the parse tree.
    * the `typeMap` will be used to generate the bytecode.

    * The visitor is also responsible for method resolution.
    * It cretes a new method signature for each method in the program.
    * when visiting a method call, it checks if the method signature is valid.
    * If the method signature is valid, it stores the mangled method signature in the `methodMap` for future use.
*/

public class SemanticsVisitor extends MiniJavaParserBaseVisitor<MiniJavaType> {
    private final List<Map<String, MiniJavaType>> symbolTable;
    private final Map<ParserRuleContext, MiniJavaType> typeMap;
    private final Map<String, List<MethodSignature>> methodSigMap;
    private final Map<MiniJavaParser.MethodCallContext, String> methodMap;

    public SemanticsVisitor() {
        this.symbolTable = new ArrayList<>();
        this.typeMap = new HashMap<>();
        this.methodSigMap = new HashMap<>();
        this.methodMap = new HashMap<>();
    }


    // This method is used when bytecode generator visit a method call
    // It will return the mangled method signature for the method call
    // The `methodMap` is maintained by SematicsVisitor
    public String getMangledMethod(MiniJavaParser.MethodCallContext ctx) {
        return methodMap.get(ctx);
    }

    // This method is used when bytecode generator need to know the type of a node
    // such as a variable, a method call, a expression etc.
    public MiniJavaType getType(ParserRuleContext ctx) {
        return typeMap.get(ctx);
    }

    private void setType(ParserRuleContext ctx, MiniJavaType type) {
        typeMap.put(ctx, type);
    }

    // When enter a new method, we clear the symbol table
    private void clearSymbolTable() {
        symbolTable.clear();
    }

    // When enter a new block, we create a new symbol table
    private void newSymbolTable() {
        symbolTable.add(new HashMap<>());
    }

    // When exit a block, we remove the symbol table
    private void removeSymbolTable() {
        symbolTable.removeLast();
    }

    private void addMethodSignature(MethodSignature methodSig) {
        var methodName = methodSig.methodName;
        if (!methodSigMap.containsKey(methodName)) {
            methodSigMap.put(methodName, new ArrayList<>());
        }
        methodSigMap.get(methodName).add(methodSig);
    }

    // This method is used to add a new variable to the symbol table
    private void setVariableType(String name, MiniJavaType type) {
        symbolTable.getLast().put(name, type);
    }

    // This method is used to get the type of a variable from the symbol table
    private MiniJavaType getVariableType(String name) {
        for (int i = symbolTable.size() - 1; i >= 0; i--) {
            var scope = symbolTable.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        throw new RuntimeException("[ERROR] Variable " + name + " not found");
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
                    || ctx.bop.getType() == MiniJavaParser.GE) {
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

    // This method is used to find the closest method signature for a method call
    // First we will find all the method signatures with the same name and parameter count
    // Then we will find the method signature with the least number of implicit conversions
    // If there is more than one method signature with the same number of implicit conversions, we will throw an error
    // If there is no method signature with the same name and parameter count, we will throw an error
    // If there is only one method signature with the same name and parameter count, we will return it
    private MethodSignature findClosestMethod(MethodSignature callSig) {
        int minConversion = Integer.MAX_VALUE;
        MethodSignature bestMethod = null;
        int bestCount = 0;  // Count of candidates with minimal conversion count
        
        // Iterate through all the method signatures with the same name
        for (var candidate : methodSigMap.get(callSig.methodName)) {
            // Check if the number of parameters is the same
            if (candidate.parameterTypes.size() != callSig.parameterTypes.size()) continue;
            // Compute how many implicit conversions are required.
            int conversionCount = getImplicitConversionCount(candidate, callSig);
            if (conversionCount == Integer.MAX_VALUE) continue;
            if (conversionCount < minConversion) {
                minConversion = conversionCount;
                bestMethod = candidate;
                bestCount = 1;
            } else if (conversionCount == minConversion) {
                bestCount++;
            }
        }
        // If more than one candidate has the same minimal conversion count, it's ambiguous.
        if (bestCount != 1) throw new RuntimeException("Ambiguous method call: " + callSig.methodName);
        return bestMethod;
    }

    private int getImplicitConversionCount(MethodSignature candidate, MethodSignature callSig) {
        int count = 0;
        int n = candidate.parameterTypes.size();
        for (int i = 0; i < n; i++) {
            MiniJavaType candidateType = candidate.parameterTypes.get(i);
            MiniJavaType callType = callSig.parameterTypes.get(i);
            // If the types are the same, no conversion is needed.
            if (candidateType.equals(callType)) {
                continue;
            }
            // If an implicit conversion is allowed from callType to candidateType, count it.
            if (callType.canImplicitlyCastTo(candidateType)) {
                count++;
            } else {
                // If conversion is not possible, mark candidate as incompatible.
                return Integer.MAX_VALUE;
            }
        }
        return count;
    }
    

    @Override
    public MiniJavaType visitCompilationUnit(MiniJavaParser.CompilationUnitContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public MiniJavaType visitMethodDeclaration(MiniJavaParser.MethodDeclarationContext ctx) {
        // When visiting a method declaration, we clear the symbol table
        // and create a new symbol table for parameters.
        clearSymbolTable();
        newSymbolTable();
        // Visit the method parameters, generate the method signature
        // the method signature is `<Class>::<MethodName>(<ParamTypes>)`
        var methodName = ctx.identifier().getText();
        var paramTypes = new ArrayList<MiniJavaType>();
        var paramList = ctx.formalParameters().formalParameterList();
        if (paramList != null) {
            for (var param : paramList.formalParameter()) {
                var type = visit(param.typeType());
                // visit the parameter so that we can add it to the symbol table
                var id = param.identifier().getText();
                setVariableType(id, type);
                paramTypes.add(type);
            }
        }
        // Note that in theory, the method signature do not include the return type,
        // but we still need to add the return type to the method signature.
        // This is because when we visit a method call, we need to use this return type
        // to check if the method call is valid.
        var returnType = ctx.VOID() != null ? new MiniJavaType("void") : visit(ctx.typeType());
        var methodSig = new MethodSignature(methodName, paramTypes, returnType);
        // Add the method signature to the method signature map
        addMethodSignature(methodSig);
        visit(ctx.methodBody);
        return null;
    }

    @Override 
    public MiniJavaType visitBlock(MiniJavaParser.BlockContext ctx) {
        // When visiting a block, we create a new symbol table
        newSymbolTable();
        visitChildren(ctx);
        // When exiting the block, we remove the symbol table
        removeSymbolTable();
        return null;
    }

    @Override
    public MiniJavaType visitBlockStatement(MiniJavaParser.BlockStatementContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public MiniJavaType visitLocalVariableDeclaration(MiniJavaParser.LocalVariableDeclarationContext ctx) {
        // Visit the variable declaration, add it to the symbol table

        // For auto type inference, the type is is the type of the expression.
        // But we need to handle a special case:
        // for DECIMAL_LITERAL, if the value is a valid value for char, we treat it as char,
        // but in auto type inference, we treat it as int.
        if (ctx.VAR() != null) {
            var id = ctx.identifier().getText();
            var exp_str = ctx.expression().getText();
            // If the expression is a DECIMAL_LITERAL, return int type
            if (exp_str.matches("[0-9]+")) {
                setVariableType(id, new MiniJavaType("int"));
                setType(ctx.expression(), new MiniJavaType("int"));
                return null;
            }
            // Else return the type of the expression
            else {
                var type = visit(ctx.expression());
                setVariableType(id, type);
                return null;
            }
        }

        // For variableDeclarator, the type of the variable is `typeType`, 
        // but we need to check if `typeType` is the same as the type of `variableDeclarator`. 
        // If not, we need to throw an error.
        var typeType = visit(ctx.typeType());
        var varDecl = ctx.variableDeclarator();
        var id = varDecl.identifier().getText();
        // If the variable is not initialized, return `typeType` directly.
        if (varDecl.variableInitializer() == null) {
            setVariableType(id, typeType);
            return null;
        }
        var varInit = visit(varDecl.variableInitializer());
        if (typeType.isEqual(varInit)) {
            setVariableType(id, typeType);
            return null;
        } else {
            throw new RuntimeException("[ERROR] Type mismatch: " + typeType.name + " != " + varInit.name);
        }
    }

    @Override
    public MiniJavaType visitVariableInitializer(MiniJavaParser.VariableInitializerContext ctx) {
        if (ctx.expression() != null) {
            var type = visit(ctx.expression());
            setType(ctx, type);
            return type;
        } else {
            var type = visit(ctx.arrayInitializer());
            setType(ctx, type);
            return type;
        }
    }

    @Override
    public MiniJavaType visitArrayInitializer(MiniJavaParser.ArrayInitializerContext ctx) {
        MiniJavaType type = null;
        var initializerList = ctx.variableInitializer();
        // If the initializerList is null, which means the array is empty,
        // we return the type of the array as `null`
        // This is because we don't know the type of the array yet.
        if (initializerList == null) return new MiniJavaType("null");
        // If the initializerList is not null, we need to check if all the initializers
        // are of the same type. If not, we need to throw an error.
        for (var initializer : initializerList) {
            var initType = visit(initializer);
            if (type == null) type = initType;
            if (!type.isEqual(initType)) {
                throw new RuntimeException("[ERROR] Type mismatch: " + type + " != " + initType);
            }
        }
        // If all the initializers are of the same type, we return the type of the array
        // as `type[]`, where `type` is the type of the initializers.
        return new MiniJavaType(type + "[]");
    }

    @Override
    public MiniJavaType visitStatement(MiniJavaParser.StatementContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public MiniJavaType visitParExpression(MiniJavaParser.ParExpressionContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public MiniJavaType visitPrimary(MiniJavaParser.PrimaryContext ctx) {
        if (ctx.identifier() != null) {
            return visit(ctx.identifier());
        } else if (ctx.literal() != null) {
            return visit(ctx.literal());
        } else if (ctx.expression() != null) {
            return visit(ctx.expression());
        }
        return null;
    }

    @Override
    public MiniJavaType visitIdentifier(MiniJavaParser.IdentifierContext ctx) {
        var id = ctx.getText();
        var type = getVariableType(id);
        setType(ctx, type);
        return type;
    }

    @Override
    public MiniJavaType visitLiteral(MiniJavaParser.LiteralContext ctx) {
        if (ctx.DECIMAL_LITERAL() != null) {
            // Check if the decimal literal is a valid value for char
            // If so, we treat it as char, else we treat it as int
            var value = ctx.DECIMAL_LITERAL().getText();
            var intValue = Integer.parseInt(value);
            if (-127 <= intValue && intValue <= 127) return new MiniJavaType("char");
            else return new MiniJavaType("int");
        } else if (ctx.CHAR_LITERAL() != null) {
            return new MiniJavaType("char");
        } else if (ctx.STRING_LITERAL() != null) {
            return new MiniJavaType("string");
        } else if (ctx.BOOL_LITERAL() != null) {
            return new MiniJavaType("boolean");
        } else if (ctx.NULL_LITERAL() != null) {
            return new MiniJavaType("null");
        }
        return null;
    }

    @Override
    public MiniJavaType visitPrimitiveType(MiniJavaParser.PrimitiveTypeContext ctx) {
        if (ctx.INT() != null) return new MiniJavaType("int");
        else if (ctx.CHAR() != null) return new MiniJavaType("char");
        else if (ctx.BOOLEAN() != null) return new MiniJavaType("boolean");
        else if (ctx.STRING() != null) return new MiniJavaType("string");
        return null;
    }

    @Override
    public MiniJavaType visitTypeType(MiniJavaParser.TypeTypeContext ctx) {
        return new MiniJavaType(ctx.getText());
    }

    private MiniJavaType visitBopExpression(MiniJavaParser.ExpressionContext ctx) {
        var lhs = visit(ctx.expression(0));
        var rhs = visit(ctx.expression(1));
        if (isConditionExp(ctx)) {
            // For logic operators, we need to check if the expression is of type boolean
            if (!lhs.isBoolean() || !rhs.isBoolean()) {
                throw new RuntimeException("[ERROR] Type mismatch: " + lhs + " and " + rhs + " should be boolean");
            }
            return new MiniJavaType("boolean");
        }
        if (ctx.bop.getType() == MiniJavaParser.QUESTION) {
            // For ternary operator, we need to make sure the condition is of type boolean,
            // and the true and false expressions are of the same type
            if (!lhs.isBoolean()) {
                throw new RuntimeException("[ERROR] Type mismatch: " + lhs + " should be boolean");
            }
            var false_exp = visit(ctx.expression(2));
            if (!false_exp.isEqual(rhs)) {
                throw new RuntimeException("[ERROR] Type mismatch: " + false_exp + " should be " + rhs);
            }
            return rhs;
        }
        if (ctx.bop.getType() == MiniJavaParser.EQUAL
                || ctx.bop.getType() == MiniJavaParser.NOTEQUAL) {
            // For equality operators, we do not care about the type,
            // return boolean type directly
            return new MiniJavaType("boolean");
        }

        if (isArithmeticAssignExp(ctx.bop.getType())) {
            // For arithmetic assign operators, we need to check if the types are the same
            // and for arithmetic operators, we need to check if the types are int or char
            // For ADD operator, the arguments can be <string, primitive> or <int/char, int/char>
            // the return type is the same as the type of the left hand side
            if (ctx.bop.getType() == MiniJavaParser.ADD_ASSIGN) {
                if (lhs.isString() && rhs.isPrimitive()) return new MiniJavaType("string");
                if (lhs.isInt() && rhs.isInt()) return new MiniJavaType("int");
                if (lhs.isChar() && rhs.isChar()) return new MiniJavaType("char");
                if (lhs.isInt() && rhs.isChar()) return new MiniJavaType("int");
                if (lhs.isChar() && rhs.isInt()) return new MiniJavaType("char");
                throw new RuntimeException("[ERROR] Type mismatch: " + lhs + " += " + rhs + " is not allowed");
            }
            if ((lhs.isChar() || lhs.isInt()) || (rhs.isChar() || rhs.isInt())) {
                return new MiniJavaType("int");
            }
            throw new RuntimeException("[ERROR] Type mismatch: " + lhs + " " + ctx.bop.getText() + " " + rhs + " is not allowed");
        }

        // For arithmetic operators, we need to check if the types are primitive types,
        // and for arithmetic operators, we need to check if the types are int or char
        // For ADD operator, the arguments can be <string, primitive> or <int/char, int/char> or <primitive, string>
        // the return type is int or string
        if (ctx.bop.getType() == MiniJavaParser.ADD) {
            if (lhs.isString() && rhs.isPrimitive()) return new MiniJavaType("string");
            if (lhs.isPrimitive() && rhs.isString()) return new MiniJavaType("string");
            if (lhs.isInt() && rhs.isInt()) return new MiniJavaType("int");
            if (lhs.isChar() && rhs.isChar()) return new MiniJavaType("char");
            if (lhs.isInt() && rhs.isChar()) return new MiniJavaType("int");
            if (lhs.isChar() && rhs.isInt()) return new MiniJavaType("int");
            throw new RuntimeException("[ERROR] Type mismatch: " + lhs + " + " + rhs + " is not allowed");
        }
        if ((lhs.isChar() || lhs.isInt()) || (rhs.isChar() || rhs.isInt())) {
            return new MiniJavaType("int");
        }
        throw new RuntimeException("[ERROR] Type mismatch: " + lhs + " " + ctx.bop.getText() + " " + rhs + " is not allowed");

    }

    @Override
    public MiniJavaType visitExpression(MiniJavaParser.ExpressionContext ctx) {
        if (ctx.primary() != null) {
            var ret = visit(ctx.primary());
            setType(ctx, ret);
            return ret;
        } else if (ctx.methodCall() != null) {
            var ret = visit(ctx.methodCall());
            setType(ctx, ret);
            return ret;
        } else if (ctx.LBRACK() != null) {
            // For array access, we need to check `array[index]`
            // we need to make sure that `array` is of type `type[]`
            // and `index` is of type `int` or `char`
            var arrayType = visit(ctx.expression(0));
            var indexType = visit(ctx.expression(1));
            if (!arrayType.isArray()) throw new RuntimeException("[ERROR] Array access: " + arrayType + " is not an array");
            if (!indexType.isInt() && !indexType.isChar()) {
                throw new RuntimeException("[ERROR] Array access: " + indexType + " is not a valid index");
            }
            var ret = new MiniJavaType(arrayType.name.substring(0, arrayType.name.length() - 2));
            setType(ctx, ret);
            return ret;
        } else if (ctx.postfix != null) {
            var ret = visit(ctx.expression(0));
            setType(ctx, ret);
            return ret;
        } else if (ctx.prefix != null) {
            var ret = visit(ctx.expression(0));
            // For logic operators, we need to check if the expression is of type boolean
            if (ctx.prefix.getType() == MiniJavaParser.BANG)
                if (!ret.isBoolean()) throw new RuntimeException("[ERROR] Type mismatch: " + ret + " should be boolean");
            setType(ctx, ret);
            return ret;
        } else if (ctx.typeType() != null) {
            var type = visit(ctx.typeType());
            var exp = visit(ctx.expression(0));
            // For explicit cast, we need to check if the expression can be casted to the type
            if (!exp.canExplicitCastTo(type)) throw new RuntimeException("[ERROR] Type mismatch: " + exp + " cannot be cast to " + type);
            setType(ctx, type);
            return type;
        } else if (ctx.creator() != null) {
            var ret = visit(ctx.creator());
            setType(ctx, ret);
            return ret;
        } else if (ctx.bop != null) {
            var ret = visitBopExpression(ctx);
            setType(ctx, ret);
            return ret;
        }  
        return null;
    }

    @Override
    public MiniJavaType visitCreator(MiniJavaParser.CreatorContext ctx) {
        var primitiveType = visit(ctx.createdName().primitiveType());
        var creatorRest = ctx.arrayCreatorRest();
        if (creatorRest.arrayInitializer() != null) {
            // For array initializer, we need to check if the type of the initializer is the same as the type of the array
            // For example, `new int[] {1, 2, 3}` is valid, but `new int[][] {1, 2, 3}` is not valid
            var initializer = visit(creatorRest.arrayInitializer());
            var dim = ctx.arrayCreatorRest().LBRACK().size();
            var declType = new MiniJavaType(primitiveType.name + "[]".repeat(dim));
            if (!initializer.isEqual(declType)) {
                throw new RuntimeException("[ERROR] Type mismatch: " + initializer + " should be " + declType);
            }
            return initializer;
        }
        else {
            // For array creation, just return the type of the array
            var dim = ctx.arrayCreatorRest().LBRACK().size();
            var declType = new MiniJavaType(primitiveType.name + "[]".repeat(dim));
            return declType;
        }
    }

    @Override
    public MiniJavaType visitMethodCall(MiniJavaParser.MethodCallContext ctx) {
        // For method call, we need to check if the method is defined
        // and if the arguments are of the same type as the parameters
        var methodName = ctx.identifier().getText();
        var paramTypes = new ArrayList<MiniJavaType>();
        for (var arg : ctx.arguments().expressionList().expression()) {
            paramTypes.add(visit(arg));
        }
        var methodSig = new MethodSignature(methodName, paramTypes);
        var closestMethod = findClosestMethod(methodSig);
        // Add the mangled method name to the method map
        // So that we can use it when generating bytecode
        var mangledMethod = closestMethod.mangle();
        methodMap.put(ctx, mangledMethod);
        return closestMethod.returnType;
    }
}
