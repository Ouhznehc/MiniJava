import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class BytecodeGenerator {
    private final List<Triple> bytecodes;

    public BytecodeGenerator() {
        this.bytecodes = new ArrayList<>();
    }

    public void emitBytecode(BytecodeType type, Integer arg1, Integer arg2) {
        bytecodes.add(new Triple(type, arg1, arg2));
    }

    public List<Triple> getBytecodes() {
        return bytecodes;
    }

    public void displayBytecodes(String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            for (var bytecode : bytecodes) {
                writer.println(bytecode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
