/**
 * This class represents a MiniJava object.
 * It can be a variable or a constant.
 * Based on the type of object, it can represent a primitive type or an array or a class.
 * For primitive types, Object value = Integer/String/...
 * For arrays, Object value = ArrayList<MiniJavaObject>
 * For classes, Object value = HashMap<MiniJavaObject>
 */
public class MiniJavaObject {
    public MiniJavaType type = null;
    public String name = null;
    public String pool = null;
    public Integer index = null;
    // At bytecode generation time, this value is only used for constants,
    // for any other object, it is null
    // But it can be used to store the value of a variable at runtime.
    public Object value = null;

    // Constructor for variable
    public MiniJavaObject(MiniJavaType type, String name) {
        this.type = type;
        this.name = name;
    }

    // Constructor for constant
    public MiniJavaObject(String type, Object value) {
        this.type = MiniJavaType.newPrimitiveType(type);
        this.value = value;
    }

    // Constructor for leftValue
    public MiniJavaObject(MiniJavaType type, Integer index) {
        this.type = type;
        this.index = index;
    }

}
