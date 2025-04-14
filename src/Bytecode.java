/**
 * This class represents a bytecode instruction in the MiniJava language.
 * It contains the type of the instruction, and optional arguments.
 * The `method` is only used for displaying the method name in the bytecode.
 */
public class Bytecode {
    public BytecodeType type = null;;
    public Integer arg1 = null;
    public Integer arg2 = null;
    public String name = null;

    public Bytecode(BytecodeType type, Integer arg1, Integer arg2) {
        this.type = type;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public Bytecode(BytecodeType type, Integer arg1) {
        this.type = type;
        this.arg1 = arg1;
    }

    public Bytecode(BytecodeType type, String name) {
        this.type = type;
        this.name = name;
    }

    public Bytecode(BytecodeType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        if (arg1 == null && arg2 == null && name == null) {
            return String.format("%s", type);
        } else if (arg2 == null && name == null) {
            return String.format("%s %d", type, arg1);
        } else if (name != null) {
            if (type == BytecodeType.OP_METHOD) {
                return String.format("========= %s =========", name);
            } else {
                return String.format("+++++++++++++++++++++++++++ %s +++++++++++++++++++++++++++", name);
            }
        } else {
            return String.format("%s %d %d", type, arg1, arg2);
        }
    }
}