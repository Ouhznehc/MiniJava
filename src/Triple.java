public class Triple {
    public BytecodeType type;
    public Integer arg1;
    public Integer arg2;

    public Triple(BytecodeType type, Integer arg1, Integer arg2) {
        this.type = type;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public String toString() {
        if (arg1 == null && arg2 == null) {
            return String.format("%s", type);
        } else if (arg2 == null) {
            return String.format("%s %d", type, arg1);
        } else {
            return String.format("%s %d %d", type, arg1, arg2);
        }
    }
}