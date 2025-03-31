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

    public final Map<String, Pools> pools;
    public final Map<String, ArrayList<Map<String, MiniJavaObject>>> symbolTable;
    public final Map<MethodSignature, String> methodMap;

    public Environment() {
        pools = new HashMap<>();
        symbolTable = new HashMap<>();
        methodMap = new HashMap<>();
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

    // Register a new method
    // 1. put the method signature into the methodMap
    // 2. create a new symbol table for the method parameters
    // 3. create a new pool for the method
    // 4. add parameters to the symbol table
    public void newMethod(MethodSignature signature, String scope, ArrayList<MiniJavaObject> parameters) {
        methodMap.put(signature, scope);

        newScope(scope);

        newSymbolTable();
        for (var parameter : parameters) newVariable(parameter.name, parameter);
    }
    
    public MiniJavaObject newVariable(String name, MiniJavaObject object) {
        object.name = name;
        object.scope = scope;
        object.index = pools.get(scope).variableIndex++;
        object.type = object.type;
        object.value = null; 

        pools.get(scope).variablePool.add(object);
        var table = symbolTable.get(scope).getLast();
        table.put(object.name, object);
        return object;
    }

    public MiniJavaObject newConstant(MiniJavaObject object) {
        object.name = null;
        object.scope = scope;
        object.index = pools.get(scope).constantIndex++;
        object.type = object.type;
        object.value = object.value;

        pools.get(scope).constantPool.add(object);
        return object;
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