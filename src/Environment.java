import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class represents the environment for MiniJava.
 * It contains the symbol table and pools for each scope.
 * The symbol table is a map of variable names to MiniJava objects.
 * The pools are used to store constant and variable pools for each scope.
 * The environment is used to manage the scopes and symbol tables during compilation.
*/
public class Environment {
    public String currentClass = null;
    public String currentPool = null;
    // The label is used to create a unique jump target.
    public Integer label = 0;
    // The tempIndex is used to create a unique temporary variable name.
    public Integer tempIndex = 0;
    // The pools are used to store constant and variable pools for each scope.
    // ! Note we use `LinkedHashMap` here to maintain the order of insertion.
    // ! So that we can display the pools in the same order as the program.
    public final LinkedHashMap<String, Pools> pools;
    // The symbol table is a map of variable names to MiniJava objects.
    // Each symbol table is a list of maps, where each map represents a symbol table for a specific block(used for variable shadowing).
    public final ArrayList<Map<String, MiniJavaObject>> symbolTable;

    public Environment() {
        pools = new LinkedHashMap<>();
        symbolTable = new ArrayList<>();
    }

    public void newSymbolTable() {
        symbolTable.add(new HashMap<>());
    }

    public void removeSymbolTable() {
        symbolTable.removeLast();
    }

    public void clearSymbolTable() {
        symbolTable.clear();
    }

    public void newPools(String pool) {
        this.currentPool = pool;
        pools.put(pool, new Pools());
    }

    public Integer newLabel() {
        return label++;
    }

    public MiniJavaObject findVariable(String id) {
        // Try to find the variable in the current scope
        for (var i = symbolTable.size() - 1; i >= 0; i--) {
            var symbol = symbolTable.get(i).get(id);
            if (symbol != null) {
                return symbol;
            }
        }
        return null;
    }

    // Register a new method
    // 1. set the current scope to the method name
    // 2. create a new pool for the method
    // 3. create a new symbol table for the method parameters
    // 4. add parameters to the symbol table
    public void newMethod(String pool, ArrayList<MiniJavaObject> parameters) {
        newPools(pool);
        newSymbolTable();
        for (var parameter : parameters) newVariable(parameter.type, parameter.name);
    }
    

    // Register a new variable
    // 1. set the variable scope to the current scope
    // 2. set the variable index to the current variable index
    // 3. add the variable to the variable pool
    // 4. add the variable to the symbol table
    public MiniJavaObject newVariable(MiniJavaType type, String name) {
        var object = new MiniJavaObject(type, name);
        object.pool = currentPool;
        object.index = pools.get(currentPool).variableIndex++;

        pools.get(currentPool).variablePool.add(object);
        var table = symbolTable.getLast();
        table.put(object.name, object);
        return object;
    }

    // Register a new constant
    // 1. set the constant scope to the current scope
    // 2. set the constant index to the current constant index
    // 3. add the constant to the constant pool
    public MiniJavaObject newConstant(String type, Object value) {
        var object = new MiniJavaObject(type, value);
        object.pool = currentPool;
        object.index = pools.get(currentPool).constantIndex++;

        pools.get(currentPool).constantPool.add(object);
        return object;
    }

    // Register a new temporary variable
    public MiniJavaObject newTemp() {
        return newVariable(MiniJavaType.newPrimitiveType("int"), tempIndex++ + "_temp");
    }

    public void displayEnvironment(String filePath) {
        try (var writer = new PrintWriter(new FileWriter(filePath))) {
            for (var entry : pools.entrySet()) {
                writer.println("================= Pools: " + entry.getKey() + " =================");
                entry.getValue().displayPools(writer);
                writer.print("\n\n\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }   
}