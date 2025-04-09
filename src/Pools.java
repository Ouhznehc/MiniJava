import java.io.PrintWriter;
import java.util.ArrayList;


/**
 * This class represents a pool of objects, including a constant pool and a variable pool.
 * The constant pool is used to store constant values, while the variable pool is used to store variables.
 * Each Method in MiniJava has its own pool of objects.
 * `constantIndex` and `variableIndex` are used when creating new objects.
 */
public class Pools {
    public ArrayList<MiniJavaObject> constantPool;
    public ArrayList<MiniJavaObject> variablePool;
    Integer constantIndex = 0;
    Integer variableIndex = 0;

    public Pools() {
        constantPool = new ArrayList<>();
        variablePool = new ArrayList<>();
    }

    public void displayPools(PrintWriter writer) {
        // Display Constant Pool
        writer.println("------------- Constant Pool ------------");
        writer.printf("%-5s %-10s %-10s%n", "Index", "Type", "Value");
        for (int i = 0; i < constantPool.size(); i++) {
            var constant = constantPool.get(i);
            writer.printf("%-5s %-10s %-10s%n", i, constant.type, constant.value);
        }
        writer.println();
    
        // Display Variable Pool
        writer.println("------------- Variable Pool ------------");
        writer.printf("%-5s %-10s %-10s %-10s%n", "Index", "Name", "Type", "Value");
        for (int i = 0; i < variablePool.size(); i++) {
            var variable = variablePool.get(i);
            writer.printf("%-5d %-10s %-10s %-10s%n", i, variable.name, variable.type, variable.value);
        }
    }
}
