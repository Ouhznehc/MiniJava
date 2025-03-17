public class Triple {
    public BytecodeType type;
    public int arg1;
    public int arg2;

    public Triple(BytecodeType type, int arg1, int arg2) {
        this.type = type;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public String toString() {
        return String.format("%s <%d> <%d>", type, arg1, arg2);
    }
}