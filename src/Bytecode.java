public class Bytecode {
    public BytecodeType type = null;;
    public Integer arg1 = null;
    public Integer arg2 = null;
    public String method = null;

    public Bytecode(BytecodeType type, Integer arg1, Integer arg2) {
        this.type = type;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public Bytecode(BytecodeType type, Integer arg1) {
        this.type = type;
        this.arg1 = arg1;
    }

    public Bytecode(BytecodeType type, String method) {
        this.type = type;
        this.method = method;
    }

    public Bytecode(BytecodeType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        if (arg1 == null && arg2 == null && method == null) {
            return String.format("%s", type);
        } else if (arg2 == null) {
            return String.format("%s %d", type, arg1);
        } else if (method != null) {
            return String.format("========= %s =========", method);
        } else {
            return String.format("%s %d %d", type, arg1, arg2);
        }
    }
}