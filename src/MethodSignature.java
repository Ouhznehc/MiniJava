import java.util.ArrayList;

public class MethodSignature {
    public String methodName;
    public ArrayList<String> parameterTypes;

    public MethodSignature(String methodName, ArrayList<String> parameterTypes) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
    }

    public String mangle() {
        var hashCode = Integer.toHexString(this.hashCode());
        return methodName + "@" + hashCode;
    }
}
