public class MiniJavaObject {
    public MiniJavaType type = null;
    public String name = null;
    public String scope = null;
    public Integer index = null;

    // Optional value, useless for bytecode generation.
    // Reserverd for runtime execution.
    public Object value = null;

    public MiniJavaObject(String type, String name) {
        this.type = new MiniJavaType(type);
        this.name = name;
    }

    public MiniJavaObject(MiniJavaType type, String name) {
        this.type = type;
        this.name = name;
    }

    public MiniJavaObject(String type, Object value) {
        this.type = new MiniJavaType(type);
        this.value = value;
    }

}
