import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class BytecodeGenerator {
    public final List<Bytecode> bytecodes;

    public BytecodeGenerator() {
        this.bytecodes = new ArrayList<>();
    }

    public void emitBytecode(BytecodeType type, Integer arg1, Integer arg2) {
        bytecodes.add(new Bytecode(type, arg1, arg2));
    }
    public void emitBytecode(BytecodeType type, Integer arg1) {
        bytecodes.add(new Bytecode(type, arg1));
    }
    public void emitBytecode(BytecodeType type, String method) {
        bytecodes.add(new Bytecode(type, method));
    }
    public void emitBytecode(BytecodeType type) {
        bytecodes.add(new Bytecode(type));
    }

    public void getVariable(MiniJavaObject variable) {
        emitBytecode(BytecodeType.OP_GET_LOCAL, variable.index);
    }

    public void setVariable(MiniJavaObject variable) {
        if (variable.type.isArray()) {
            emitBytecode(BytecodeType.OP_SET_INDEX);
            return;
        }
        else emitBytecode(BytecodeType.OP_SET_LOCAL, variable.index);
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
