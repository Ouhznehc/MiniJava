import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.ParserRuleContext;

/***
    * This class is responsible for semantic analysis of the MiniJava program.
    * It extends the MiniJavaParserBaseVisitor class to visit each node in the parse tree.
    * The visitor checks for semantic errors and collects type information.

    * The visitor is also responsible for method resolution.
    * It cretes a new method signature for each method in the program.
    * when visiting a method call, it checks if the method signature is valid.
    * If the method signature is valid, it stores the mangled method signature in the `methodMap` for future use.
    ! Note that all global methods are treated methods in a class named `global`, which does not extend any class.
*/

public class SemanticsVisitor extends MiniJavaParserBaseVisitor<MiniJavaType> {
    // `symbolTable` is a stack of maps, where each map represents a scope/block.
    // When enter a new method, we clear the WHOLE symbol table.
    private final List<Map<String, MiniJavaType>> symbolTable;
    // `typeMap` is used to store the type of each node in the parse tree.
    // It is used to check the type of a variable, a method call, an expression, etc.
    private final Map<ParserRuleContext, MiniJavaType> typeMap;
    // `methodMap` is used to store the mangled method signature for each method call.
    // It is used to generate the bytecode for the method call.
    private final Map<ParserRuleContext, String> methodMap;
    // `classMethodMap` is used to store the method signatures for each class in the program.
    private final Map<String, List<MethodSignature>> classMethodMap;
    // `classFieldMap` is used to store the field types for each class in the program.
    private final Map<String, Map<String, MiniJavaType>> classFieldMap;
    // `parentClassMap` is used to store the parent class for each class in the program.
    private final Map<String, String> parentClassMap;
    private String currentClassName = null;

    public SemanticsVisitor() {
        this.symbolTable = new ArrayList<>();
        this.typeMap = new HashMap<>();
        this.methodMap = new HashMap<>();
        this.classMethodMap = new HashMap<>();
        this.classFieldMap = new HashMap<>();
        this.parentClassMap = new HashMap<>();
    }


    // This method is used when bytecode generator visit a method call
    // It will return the mangled method signature for the method call
    // The `methodMap` is maintained by SematicsVisitor
    public String getMangledMethod(ParserRuleContext ctx) {
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

    // When enter a new block, we create a new symbol table
    private void newSymbolTable() {
        symbolTable.add(new HashMap<>());
    }

    // When exit a block, we remove the symbol table
    private void removeSymbolTable() {
        symbolTable.removeLast();
    }

    // When enter a new method, we clear the symbol table
    private void clearSymbolTable() {
        symbolTable.clear();
    }

    private void addMethodSignature(MethodSignature methodSig) {
        var className = methodSig.className;
        if (!classMethodMap.containsKey(className)) {
            classMethodMap.put(className, new ArrayList<>());
        }
        classMethodMap.get(className).add(methodSig);
    }

    // This method is used to add a new variable to the symbol table
    private void setVariableType(String name, MiniJavaType type) {
        symbolTable.getLast().put(name, type);
    }

    // This method is used to get the type of a variable
    private MiniJavaType getVariableType(String name) {
        // First we treat it as a local variable
        for (int i = symbolTable.size() - 1; i >= 0; i--) {
            var scope = symbolTable.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        // Then we treat it as a class field
        var currentClass = currentClassName;
        while (currentClass != null) {
            var classFields = classFieldMap.get(currentClass);
            if (classFields.containsKey(name)) {
                return classFields.get(name);
            }
            currentClass = parentClassMap.get(currentClass);
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

    private boolean compareExactMethod(MethodSignature candidate, MethodSignature callSig) {
        if (!candidate.methodName.equals(callSig.methodName)) return false;
        if (candidate.parameterTypes.size() != callSig.parameterTypes.size()) return false;
        int size = candidate.parameterTypes.size();
        for (int i = 0; i < size; i++) {
            var paramType = candidate.parameterTypes.get(i);
            var callType = callSig.parameterTypes.get(i);
            if (!callType.isEqual(paramType)) return false;
        }
        return true;
    }

    private MethodSignature countExactMethod(MethodSignature callSig, String className) {
        int exactMethodCount = 0;
        MethodSignature exactMethod = null;

        var methodSignatures = classMethodMap.get(className);
        if (methodSignatures != null) {
            for (var methodSig : methodSignatures) {
                if (compareExactMethod(methodSig, callSig)) {
                    exactMethod = methodSig;
                    exactMethodCount++;
                }
            }
        }
        if (exactMethodCount > 1) throw new RuntimeException("Ambiguous method call: " + callSig.mangle());
        return exactMethod;
    }

    private MethodSignature findExactMethod(MethodSignature callSig, boolean isDotMethodCall) {
        var className = callSig.className;
        // First we check if the method is in the current class or its super class.
        while (className != null) {
           var exactMethod = countExactMethod(callSig, className);
            if (exactMethod != null) {
                return exactMethod;
            }
            className = parentClassMap.get(className);
        }
        if (isDotMethodCall) {
            throw new RuntimeException("Method " + callSig.mangle() + " not found.");
        }
        // Then we check if the method is in the global class.
        var exactMethod = countExactMethod(callSig, "global");
        return exactMethod;
    }

    private MethodSignature findOverloadedMethod(MethodSignature callSig, boolean isDotMethodCall) {
        var className = callSig.className;

        int minConversion = Integer.MAX_VALUE;
        MethodSignature bestMethod = null;
        int bestCount = 0; 

        while (className != null) {
            var methodSignatures = classMethodMap.get(className);
            if (methodSignatures != null) {
                for (var methodSig : methodSignatures) {
                    int conversionCount = getImplicitConversionCount(methodSig, callSig);
                    if (conversionCount == Integer.MAX_VALUE) continue;
                    if (conversionCount < minConversion) {
                        minConversion = conversionCount;
                        bestMethod = methodSig;
                        bestCount = 1;
                    } else if (conversionCount == minConversion) {
                        bestCount++;
                    }
                }
            }
            className = parentClassMap.get(className);
        }

        if (isDotMethodCall) {
            if (bestCount == 0) throw new RuntimeException("Method " + callSig.mangle() + " not found.");
            if (bestCount > 1) throw new RuntimeException("Ambiguous method call: " + callSig.mangle());
            return bestMethod;
        }
        // Find in the global class
        var methodSignatures = classMethodMap.get(className);
        if (methodSignatures != null) {
            for (var methodSig : methodSignatures) {
                int conversionCount = getImplicitConversionCount(methodSig, callSig);
                if (conversionCount == Integer.MAX_VALUE) continue;
                if (conversionCount < minConversion) {
                    minConversion = conversionCount;
                    bestMethod = methodSig;
                    bestCount = 1;
                } else if (conversionCount == minConversion) {
                    bestCount++;
                }
            }
        }
        if (bestCount == 0) throw new RuntimeException("Method " + callSig.mangle() + " not found.");
        if (bestCount > 1) throw new RuntimeException("Ambiguous method call: " + callSig.mangle());
        return bestMethod;
    }

    // This method is used to find the closest method signature for a method call
    // First we will find all the method signatures with the same name and parameter count
    // Then we will find the method signature with the least number of implicit conversions
    // If there is more than one method signature with the same number of implicit conversions, we will throw an error
    // If there is no method signature with the same name and parameter count, we will throw an error
    // If there is only one method signature with the same name and parameter count, we will return it
    private MethodSignature findClosestMethod(MethodSignature callSig, boolean isDotMethodCall) {
        MethodSignature exactlyMethodSignature = findExactMethod(callSig, isDotMethodCall);
        if (exactlyMethodSignature != null) {
            return exactlyMethodSignature;
        }
        else return findOverloadedMethod(callSig, isDotMethodCall);
    }

    private Integer getImplicitConversionCount(MethodSignature candidate, MethodSignature callSig) {
        if (!candidate.methodName.equals(callSig.methodName)) return Integer.MAX_VALUE;
        if (candidate.parameterTypes.size() != callSig.parameterTypes.size()) return Integer.MAX_VALUE;
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


    private void preVisitMethodDeclaration(MiniJavaParser.MethodDeclarationContext ctx, String className) {
        var methodName = ctx.identifier().getText();
        var paramTypes = new ArrayList<MiniJavaType>();
        var paramList = ctx.formalParameters().formalParameterList();
        if (paramList != null) {
            for (var param : paramList.formalParameter()) {
                var type = visit(param.typeType());
                paramTypes.add(type);
            }
        }
        var methodSig = new MethodSignature(className, methodName, paramTypes);
        addMethodSignature(methodSig);
    }

    private void preVisitConstructorDeclaration(MiniJavaParser.ConstructorDeclarationContext ctx, String className) {
        var methodName = ctx.identifier().getText();
        var paramTypes = new ArrayList<MiniJavaType>();
        var paramList = ctx.formalParameters().formalParameterList();
        if (paramList != null) {
            for (var param : paramList.formalParameter()) {
                var type = visit(param.typeType());
                paramTypes.add(type);
            }
        }
        var methodSig = new MethodSignature(className, methodName, paramTypes);
        addMethodSignature(methodSig);
    }

    private void preVisitFieldDeclaration(MiniJavaParser.FieldDeclarationContext ctx, String className) {
        var fieldType = visit(ctx.typeType());
        var fieldName = ctx.variableDeclarator().identifier().getText();
        if (!classFieldMap.containsKey(className)) {
            classFieldMap.put(className, new HashMap<>());
        }
        classFieldMap.get(className).put(fieldName, fieldType);
    }

    private void preVisitClassBodyDeclaration(MiniJavaParser.ClassBodyDeclarationContext ctx, String className) {
        if (ctx.methodDeclaration() != null) preVisitMethodDeclaration(ctx.methodDeclaration(), className);
        if (ctx.constructorDeclaration() != null) preVisitConstructorDeclaration(ctx.constructorDeclaration(), className);
        if (ctx.fieldDeclaration() != null) preVisitFieldDeclaration(ctx.fieldDeclaration(), className);
    }
    
    private void preVisitClassDeclaration(MiniJavaParser.ClassDeclarationContext ctx) {
        var className = ctx.identifier().getText();
        String parentClassName = null;
        if (ctx.parentClassDeclaration() != null)
            parentClassName = ctx.parentClassDeclaration().identifier().getText();
        parentClassMap.put(className, parentClassName);
        var classBodyDecls = ctx.classBody().classBodyDeclaration();
        for (var classBodeDecl: classBodyDecls) preVisitClassBodyDeclaration(classBodeDecl, className);
    }
    
    // This is really a heavy and dirty job for `compilationUnit`.
    // Because MiniJava supports used before declaration,
    // we have to pre-visit the whole tree to find all the method signatures and class fields first.
    // And then add them to the corresponding data structure.
    @Override
    public MiniJavaType visitCompilationUnit(MiniJavaParser.CompilationUnitContext ctx) {
        var classDecls = ctx.classDeclaration();
        var methodDecls = ctx.methodDeclaration();

        for (var classDecl : classDecls) {
            preVisitClassDeclaration(classDecl);
        }
        parentClassMap.put("global", null);
        classFieldMap.put("global", new HashMap<>());
        for (var methodDecl : methodDecls) {
            preVisitMethodDeclaration(methodDecl, "global");
        }
        // After pre-visiting the whole tree, we can visit the tree again to check for semantic errors.
        for (var classDecl : classDecls) {
            visitClassDeclaration(classDecl);
        }
        for (var methodDecl : methodDecls) {
            currentClassName = "global";
            visitMethodDeclaration(methodDecl);
        }
        return null;
    }

    @Override
    public MiniJavaType visitFormalParameters(MiniJavaParser.FormalParametersContext ctx) {
        var paramTypes = new ArrayList<MiniJavaType>();
        var paramList = ctx.formalParameterList();
        if (paramList != null) {
            for (var param : paramList.formalParameter()) {
                var type = visit(param.typeType());
                var id = param.identifier().getText();
                setVariableType(id, type);
                paramTypes.add(type);
            }
        }
        return null;
    }

    @Override
    public MiniJavaType visitClassDeclaration(MiniJavaParser.ClassDeclarationContext ctx) {
        var className = ctx.identifier().getText();
        currentClassName = className;
        clearSymbolTable();
        visit(ctx.classBody());
        return null;
    }

    @Override
    public MiniJavaType visitClassBody(MiniJavaParser.ClassBodyContext ctx) {
        newSymbolTable();
        visitChildren(ctx);
        removeSymbolTable();
        return null;
    }

    @Override 
    public MiniJavaType visitFieldDeclaration(MiniJavaParser.FieldDeclarationContext ctx) {
        // For variableDeclarator, the type of the variable is `typeType`, 
        // but we need to check if `typeType` is the same as the type of `variableDeclarator`. 
        // If not, we need to throw an error.
        var typeType = visit(ctx.typeType());
        var varDecl = ctx.variableDeclarator();
        // If the variable is not initialized, return `typeType` directly.
        if (varDecl.variableInitializer() == null) {
            return null;
        }
        var varInit = visit(varDecl.variableInitializer());
        if (varInit.canExplicitCastTo(typeType)) {
            return null;
        } else {
            throw new RuntimeException("[ERROR] Type mismatch: " + typeType + " != " + varInit);
        }
    }

    @Override
    public MiniJavaType visitMethodDeclaration(MiniJavaParser.MethodDeclarationContext ctx) {
        // When visiting a method declaration, we first clear the symbol table,
        // and the create a new symbol table for the method parameters.
        clearSymbolTable();
        newSymbolTable();

        visit(ctx.formalParameters());

        visit(ctx.methodBody);
        return null;
    }

    @Override
    public MiniJavaType visitConstructorDeclaration(MiniJavaParser.ConstructorDeclarationContext ctx) {
        // When visiting a constructor declaration, we first clear the symbol table,
        // and the create a new symbol table for the method parameters.
        clearSymbolTable();
        newSymbolTable();

        visit(ctx.formalParameters());

        visit(ctx.constructorBody);
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
        if (ctx.VAR() != null) {
            var id = ctx.identifier().getText();
            var type = visit(ctx.expression());
            setVariableType(id, type);
            return null;
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
        if (varInit.canExplicitCastTo(typeType)) {
            setVariableType(id, typeType);
            return null;
        } else {
            throw new RuntimeException("[ERROR] Type mismatch: " + typeType + " != " + varInit);
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
        if (initializerList == null) return MiniJavaType.newPrimitiveType("null");
        // If the initializerList is not null, we need to check if all the initializers
        // are of the same type. If not, we need to throw an error.
        for (var initializer : initializerList) {
            var initType = visit(initializer);
            if (type == null) type = initType;
            if (!initType.canImplicitlyCastTo(type)) {
                throw new RuntimeException("[ERROR] Type mismatch: " + type + " != " + initType);
            }
        }
        // If all the initializers are of the same type, we return the type of the array
        // as `type[]`, where `type` is the type of the initializers.
        return new MiniJavaType(type.primitiveType, type.classType, type.arrayDimension + 1);
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
        } else if (ctx.THIS() != null) {
            if (currentClassName.equals("global")) {
                throw new RuntimeException("[ERROR] Cannot use THIS in global scope");
            }
            return MiniJavaType.newClassType(currentClassName);
        } else if (ctx.SUPER() != null) {
            if (currentClassName.equals("global")) {
                throw new RuntimeException("[ERROR] Cannot use SUPER in global scope");
            }
            // For super, we need to check if the current class has a parent class
            // If not, we need to throw an error.
            var superClassName = parentClassMap.get(currentClassName);
            if (superClassName == null) {
                throw new RuntimeException("[ERROR] Super: " + currentClassName + " has no parent class");
            }
            return MiniJavaType.newClassType(superClassName);
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
        // ! Note that in course lab, we suppurt to treat DECIMAL_LITERAL as char
        // ! but in this implementation, we only support it as int
        if (ctx.DECIMAL_LITERAL() != null) {
            return MiniJavaType.newPrimitiveType("int");
        } else if (ctx.CHAR_LITERAL() != null) {
            return MiniJavaType.newPrimitiveType("char");
        } else if (ctx.STRING_LITERAL() != null) {
            return MiniJavaType.newPrimitiveType("string");
        } else if (ctx.BOOL_LITERAL() != null) {
            return MiniJavaType.newPrimitiveType("boolean");
        } else if (ctx.NULL_LITERAL() != null) {
            return MiniJavaType.newPrimitiveType("null");
        }
        return null;
    }

    @Override
    public MiniJavaType visitPrimitiveType(MiniJavaParser.PrimitiveTypeContext ctx) {
        if (ctx.INT() != null) return MiniJavaType.newPrimitiveType("int");
        else if (ctx.CHAR() != null) return MiniJavaType.newPrimitiveType("char");
        else if (ctx.BOOLEAN() != null) return MiniJavaType.newPrimitiveType("boolean");
        else if (ctx.STRING() != null) return MiniJavaType.newPrimitiveType("string");
        return null;
    }

    @Override
    public MiniJavaType visitTypeType(MiniJavaParser.TypeTypeContext ctx) {
        var dimension = ctx.LBRACK().size();
        if (ctx.primitiveType() != null) {
            var primitive = visit(ctx.primitiveType());
            var ret = new MiniJavaType(primitive.primitiveType, null, dimension);
            setType(ctx, ret);
            return ret;
        } else if (ctx.identifier() != null) {
            var id = visit(ctx.identifier());
            var ret = new MiniJavaType(null, id.classType, dimension);
            setType(ctx, ret);
            return ret;
        }
        throw new RuntimeException("[ERROR] Unknown typeType: " + ctx.getText());
    }

    private MiniJavaType visitBopExpression(MiniJavaParser.ExpressionContext ctx) {
        var lhs = visit(ctx.expression(0));
        var rhs = visit(ctx.expression(1));
        if (isConditionExp(ctx)) {
            // For logic operators, we need to check if the expression is of type boolean
            if (!lhs.isBoolean() || !rhs.isBoolean()) {
                throw new RuntimeException("[ERROR] Type mismatch: " + lhs + " and " + rhs + " should be boolean");
            }
            return MiniJavaType.newPrimitiveType("boolean");
        }
        if (ctx.bop.getType() == MiniJavaParser.QUESTION) {
            // For ternary operator, we need to make sure the condition is of type boolean,
            // and the true and false expressions are of the same type
            if (!lhs.isBoolean()) {
                throw new RuntimeException("[ERROR] Type mismatch: " + lhs + " should be boolean");
            }
            var false_exp = visit(ctx.expression(2));
            if (!false_exp.canImplicitlyCastTo(rhs) && !rhs.canImplicitlyCastTo(false_exp)) {
                throw new RuntimeException("[ERROR] Type mismatch: " + false_exp + " should be " + rhs);
            }
            return rhs;
        }
        if (ctx.bop.getType() == MiniJavaParser.EQUAL
                || ctx.bop.getType() == MiniJavaParser.NOTEQUAL) {
            // For equality operators, we do not care about the type,
            // return boolean type directly
            return MiniJavaType.newPrimitiveType("boolean");
        }

        if (isArithmeticAssignExp(ctx.bop.getType())) {
            // For arithmetic assign operators, we need to check if the types are the same
            // and for arithmetic operators, we need to check if the types are int or char
            // For ADD operator, the arguments can be <string, primitive> or <int/char, int/char>
            // the return type is the same as the type of the left hand side
            if (ctx.bop.getType() == MiniJavaParser.ADD_ASSIGN) {
                if (lhs.isString() && rhs.isPrimitive()) return MiniJavaType.newPrimitiveType("string");
                if (lhs.isInt() && rhs.isInt()) return MiniJavaType.newPrimitiveType("int");
                if (lhs.isChar() && rhs.isChar()) return MiniJavaType.newPrimitiveType("char");
                if (lhs.isInt() && rhs.isChar()) return MiniJavaType.newPrimitiveType("int");
                if (lhs.isChar() && rhs.isInt()) return MiniJavaType.newPrimitiveType("char");
                throw new RuntimeException("[ERROR] Type mismatch: " + lhs + " += " + rhs + " is not allowed");
            }
            if (ctx.bop.getType() == MiniJavaParser.ASSIGN) {
                if (rhs.canImplicitlyCastTo(lhs)) return lhs;
                throw new RuntimeException("[ERROR] Type mismatch: " + lhs + " = " + rhs + " is not allowed");
            }
            if ((lhs.isChar() || lhs.isInt()) || (rhs.isChar() || rhs.isInt())) {
                return MiniJavaType.newPrimitiveType("int");
            }
            throw new RuntimeException("[ERROR] Type mismatch: " + lhs + " " + ctx.bop.getText() + " " + rhs + " is not allowed");
        }

        // For arithmetic operators, we need to check if the types are primitive types,
        // and for arithmetic operators, we need to check if the types are int or char
        // For ADD operator, the arguments can be <string, primitive> or <int/char, int/char> or <primitive, string>
        // the return type is int or string
        if (ctx.bop.getType() == MiniJavaParser.ADD) {
            if (lhs.isString() && rhs.isPrimitive()) return MiniJavaType.newPrimitiveType("string");
            if (lhs.isPrimitive() && rhs.isString()) return MiniJavaType.newPrimitiveType("string");
            if (lhs.isInt() && rhs.isInt()) return MiniJavaType.newPrimitiveType("int");
            if (lhs.isChar() && rhs.isChar()) return MiniJavaType.newPrimitiveType("char");
            if (lhs.isInt() && rhs.isChar()) return MiniJavaType.newPrimitiveType("int");
            if (lhs.isChar() && rhs.isInt()) return MiniJavaType.newPrimitiveType("int");
            throw new RuntimeException("[ERROR] Type mismatch: " + lhs + " + " + rhs + " is not allowed");
        }
        if ((lhs.isChar() || lhs.isInt()) || (rhs.isChar() || rhs.isInt())) {
            return MiniJavaType.newPrimitiveType("int");
        }
        throw new RuntimeException("[ERROR] Type mismatch: " + lhs + " " + ctx.bop.getText() + " " + rhs + " is not allowed");

    }

    private MiniJavaType visitClassMethodCall(MiniJavaParser.MethodCallContext ctx, String className, boolean isDotMethodCall) {
        var methodName = ctx.identifier().getText();
        var paramTypes = new ArrayList<MiniJavaType>();
        if (ctx.arguments().expressionList() != null)
            for (var arg : ctx.arguments().expressionList().expression())
                paramTypes.add(visit(arg));
        var methodSig = new MethodSignature(className, methodName, paramTypes);
        var closestMethod = findClosestMethod(methodSig, isDotMethodCall);
        var mangledMethod = closestMethod.mangle();
        methodMap.put(ctx, mangledMethod);
        return MiniJavaType.newAnyType();
    }

    private MiniJavaType visitDotExp(MiniJavaParser.ExpressionContext ctx) {
        var exp = visit(ctx.expression(0));
        if (!exp.isClass()) throw new RuntimeException("[ERROR] Dot Expression: " + exp + " is not a class");
        if (ctx.identifier() != null) {
            var id = ctx.identifier().getText();
            var thisClass = exp.classType;
            while (thisClass != null) {
                var classFields = classFieldMap.get(thisClass);
                if (classFields.containsKey(id)) {
                    return classFields.get(id);
                }
                thisClass = parentClassMap.get(thisClass);
            }
            throw new RuntimeException("[ERROR] Dot Expression: " + id + " not found in class " + exp.classType);
        } else {
            return visitClassMethodCall(ctx.methodCall(), exp.classType, true);
        }
    }

    @Override
    public MiniJavaType visitExpression(MiniJavaParser.ExpressionContext ctx) {
        if (ctx.primary() != null) {
            var ret = visit(ctx.primary());
            setType(ctx, ret);
            return ret;
        } else if (ctx.bop != null && ctx.bop.getType() == MiniJavaParser.DOT){
            var ret = visitDotExp(ctx);
            setType(ctx, ret);
            return ret;
        } else if (ctx.methodCall() != null) {
            var ret = visitClassMethodCall(ctx.methodCall(), currentClassName, false);
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
            var ret = new MiniJavaType(arrayType.primitiveType, arrayType.classType, arrayType.arrayDimension - 1);
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

    private MiniJavaType visitArrayCreator(MiniJavaParser.CreatorContext ctx) {
        MiniJavaType createdName = null;
        if (ctx.createdName().primitiveType() != null) createdName = visit(ctx.createdName().primitiveType());
        else createdName = MiniJavaType.newClassType(ctx.createdName().identifier().getText());

        var creatorRest = ctx.arrayCreatorRest();
        if (creatorRest.arrayInitializer() != null) {
            // For array initializer, we need to check if the type of the initializer is the same as the type of the array
            // For example, `new int[] {1, 2, 3}` is valid, but `new int[][] {1, 2, 3}` is not valid
            var initializer = visit(creatorRest.arrayInitializer());
            var dim = ctx.arrayCreatorRest().LBRACK().size();
            var declType = new MiniJavaType(createdName.primitiveType, createdName.classType, dim);
            if (!initializer.canExplicitCastTo(declType)) {
                throw new RuntimeException("[ERROR] Type mismatch: " + initializer + " should be " + declType);
            }
            return initializer;
        }
        else {
            // For array creation, just return the type of the array
            var dim = ctx.arrayCreatorRest().LBRACK().size();
            var declType = new MiniJavaType(createdName.primitiveType, createdName.classType, dim);
            return declType;
        }
    }

    private MiniJavaType visitClassCreator(MiniJavaParser.CreatorContext ctx) {
        var className = ctx.createdName().identifier().getText();
        var paramTypes = new ArrayList<MiniJavaType>();
        if (ctx.classCreatorRest().expressionList() != null)
            for (var arg : ctx.classCreatorRest().expressionList().expression())
                paramTypes.add(visit(arg));
        var methodSig = new MethodSignature(className, className, paramTypes);
        var constructor = findOverloadedMethod(methodSig, false);
        if (constructor == null) {
            throw new RuntimeException("[ERROR] Constructor for" + ctx.getText() + " not found");
        }
        var mangledMethod = constructor.mangle();
        methodMap.put(ctx, mangledMethod);
        return MiniJavaType.newClassType(className);
    }

    @Override
    public MiniJavaType visitCreator(MiniJavaParser.CreatorContext ctx) {
        if (ctx.arrayCreatorRest() != null) {
            return visitArrayCreator(ctx);
        } else {
            return visitClassCreator(ctx);
        }
       
    }
}
