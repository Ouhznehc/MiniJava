import java.io.PrintWriter;
import java.util.ArrayList;

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
