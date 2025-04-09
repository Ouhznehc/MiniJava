import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents the environment for MiniJava.
 * It contains the symbol table and pools for each scope.
 * The symbol table is a map of variable names to MiniJava objects.
 * The pools are used to store constant and variable pools for each scope.
 * The environment is used to manage the scopes and symbol tables during compilation.
*/
public class Environment {
    public String scope = null;
    // The label is used to create a unique jump target.
    public Integer label = 0;
    // The tempIndex is used to create a unique temporary variable name.
    public Integer tempIndex = 0;
    // The pools are used to store constant and variable pools for each scope.
    public final Map<String, Pools> pools;
    // The symbol table is a map of variable names to MiniJava objects.
    // Each scope has its own symbol table.
    // Each symbol table is a list of maps, where each map represents a symbol table for a specific block(used for variable shadowing).
    public final Map<String, ArrayList<Map<String, MiniJavaObject>>> symbolTable;

    public Environment() {
        pools = new HashMap<>();
        symbolTable = new HashMap<>();
    }

    public void newSymbolTable() {
        symbolTable.get(scope).add(new HashMap<>());
    }

    public void removeSymbolTable() {
        symbolTable.get(scope).removeLast();
    }

    public void newScope(String scope) {
        this.scope = scope;
        pools.put(scope, new Pools());
        symbolTable.put(scope, new ArrayList<>());
    }

    public Integer newLabel() {
        return label++;
    }

    public MiniJavaObject findVariable(String id) {
        // Try to find the variable in the current scope
        var table = symbolTable.get(scope);
        for (var i = table.size() - 1; i >= 0; i--) {
            var symbol = table.get(i).get(id);
            if (symbol != null) {
                return symbol;
            }
        }
        throw new RuntimeException("Variable " + id + " not found");
    }

    // Register a new method
    // 1. set the current scope to the method name
    // 2. create a new pool for the method
    // 3. create a new symbol table for the method parameters
    // 4. add parameters to the symbol table
    public void newMethod(String scope, ArrayList<MiniJavaObject> parameters) {
        newScope(scope);
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
        object.scope = scope;
        object.index = pools.get(scope).variableIndex++;

        pools.get(scope).variablePool.add(object);
        var table = symbolTable.get(scope).getLast();
        table.put(object.name, object);
        return object;
    }

    // Register a new constant
    // 1. set the constant scope to the current scope
    // 2. set the constant index to the current constant index
    // 3. add the constant to the constant pool
    public MiniJavaObject newConstant(String type, Object value) {
        var object = new MiniJavaObject(type, value);
        object.scope = scope;
        object.index = pools.get(scope).constantIndex++;

        pools.get(scope).constantPool.add(object);
        return object;
    }

    // Register a new temporary variable
    public MiniJavaObject newTemp() {
        return newVariable(new MiniJavaType("int"), tempIndex++ + "_temp");
    }

    public void displayEnvironment(String filePath) {
        try (var writer = new PrintWriter(new FileWriter(filePath))) {
            for (var entry : pools.entrySet()) {
                writer.println("================= Scope: " + entry.getKey() + " =================");
                entry.getValue().displayPools(writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }   
}