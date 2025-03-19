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
    public final Map<String, Pools> pools;
    public final Map<String, ArrayList<Map<String, MiniJavaObject>>> symbolTable;

    public Environment() {
        pools = new HashMap<>();
        symbolTable = new HashMap<>();
        pools.put("0_global", new Pools());
        symbolTable.put("0_global", new ArrayList<>());
    }
    
    public MiniJavaObject newVariable(String scope, String name, MiniJavaObject object) {
        object.name = name;
        object.scope = scope;
        object.index = pools.get(scope).variableIndex++;
        object.type = object.type;
        object.value = null; 

        pools.get(scope).variablePool.add(object);
        var table = symbolTable.get(scope).getLast();
        table.put(name, object);
        return object;
    }

    public MiniJavaObject newConstant(String scope, MiniJavaObject object) {
        object.name = null;
        object.scope = scope;
        object.index = pools.get(scope).constantIndex++;
        object.type = MiniJavaType.CONSTANT;
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