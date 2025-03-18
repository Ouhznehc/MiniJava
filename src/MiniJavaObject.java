public class MiniJavaObject {
    public MiniJavaType type = null;
    public Object value = null;
    public String name = null;
    public String scope = null;
    public Integer index = null;

    public MiniJavaObject(MiniJavaType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public boolean isGlobal() {
        return scope.equals("0_global");
    }
    public boolean isLocal() {
        return !isGlobal();
    }
}
