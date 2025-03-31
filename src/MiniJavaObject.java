public class MiniJavaObject {
    public MiniJavaType type = null;

    // Optional value, useless for bytecode generation.
    // Reserverd for runtime execution.
    public Object value = null;

    public String name = null;
    public String scope = null;
    public Integer index = null;

    public MiniJavaObject(String type) {
        this.type = new MiniJavaType(type);
    }

    public MiniJavaObject(String type, String name) {
        this.type = new MiniJavaType(type);
        this.name = name;
    }

    public MiniJavaObject(MiniJavaType type, String name) {
        this.type = new MiniJavaType(type.name);
        this.name = name;
    }

    public MiniJavaObject(String type, Object value) {
        this.type = new MiniJavaType(type);
        this.value = value;
    }

    public boolean isGlobal() {
        return scope.equals("0_global");
    }
    public boolean isLocal() {
        return !isGlobal();
    }
}
