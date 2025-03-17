import java.util.ArrayList;
import java.util.List;

public class BytecodeGenerator {
    private final List<Triple> bytecodes;

    public BytecodeGenerator() {
        this.bytecodes = new ArrayList<>();
    }

    public void emitBytecode(BytecodeType type, int arg1, int arg2) {
        bytecodes.add(new Triple(type, arg1, arg2));
    }

    public List<Triple> getBytecodes() {
        return bytecodes;
    }

    public void displayBytecodes() {
        for (var bytecode : bytecodes) {
            System.out.println(bytecode);
        }
    }
}
