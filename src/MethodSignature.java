import java.util.ArrayList;

/**
 * This class represents a method signature in MiniJava.
 * It contains the class name, method name, return type, and parameter types.
 * Note that in theory, we don't need to store the return type,
 * but for convenience, we store it here for type checking in SemanticsVisitor.java.
 * `mangle()` is used to create a unique identifier for the method signature,
 * so that in method overloading, we can differentiate between methods with the same name but different parameter types.
 */
public class MethodSignature {
    public String methodName;
    public MiniJavaType returnType;
    public ArrayList<MiniJavaType> parameterTypes;

    public MethodSignature(String methodName, ArrayList<MiniJavaType> parameterTypes, MiniJavaType returnType) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
    }

    public MethodSignature(String methodName, ArrayList<MiniJavaType> parameterTypes) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.returnType = null;
    }

    public String mangle() {
        var hashCode = Integer.toHexString(this.hashCode());
        return methodName + "@" + hashCode;
    }
}
