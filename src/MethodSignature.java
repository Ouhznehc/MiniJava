import java.util.ArrayList;

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
