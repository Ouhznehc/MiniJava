import java.util.ArrayList;

/**
 * This class represents a method signature in MiniJava.
 * It contains the class name, method name, and parameter types.
 * `mangle()` is used to create a unique identifier for the method signature,
 * so that in method overloading, we can differentiate between methods with the same name but different parameter types.
 */
public class MethodSignature {
    public String className;
    public String methodName;
    public ArrayList<MiniJavaType> parameterTypes;

    public MethodSignature(String className, String methodName, ArrayList<MiniJavaType> parameterTypes) {
        this.className = className;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
    }


    public String mangle() {
        StringBuilder sb = new StringBuilder();
        sb.append(className);
        sb.append("::");
        sb.append(methodName);
        sb.append("(");
        for (MiniJavaType type : parameterTypes) {
            sb.append(type.toString());
        }
        sb.append(")");
        return sb.toString();
    }
}
