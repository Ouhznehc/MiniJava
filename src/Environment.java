import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// this class stores the whole environment for bytecode
// specifically we create a `symbolTableStack` for each function,
// and we have a global scope named `0_global`.
// Because IDENTIFIER must start with letter not digit,
// this global name would not conflict with other function name.
public class Environment {
    public String scope = null;
    public Integer label = 0;
    public Integer tempIndex = 0;

    public final Map<String, Pools> pools;
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
    // 1. create a new symbol table for the method parameters
    // 2. create a new pool for the method
    // 3. add parameters to the symbol table
    public void newMethod(String scope, ArrayList<MiniJavaObject> parameters) {
        newScope(scope);
        newSymbolTable();
        for (var parameter : parameters) newVariable(parameter.type, parameter.name);
    }
    
    public MiniJavaObject newVariable(MiniJavaType type, String name) {
        var object = new MiniJavaObject(type, name);
        object.scope = scope;
        object.index = pools.get(scope).variableIndex++;

        pools.get(scope).variablePool.add(object);
        var table = symbolTable.get(scope).getLast();
        table.put(object.name, object);
        return object;
    }

    public MiniJavaObject newConstant(String type, Object value) {
        var object = new MiniJavaObject(type, value);
        object.scope = scope;
        object.index = pools.get(scope).constantIndex++;

        pools.get(scope).constantPool.add(object);
        return object;
    }

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