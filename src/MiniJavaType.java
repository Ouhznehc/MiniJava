import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class MiniJavaType {
    public String name;

    public MiniJavaType(String type) {
        this.name = type;
    }

    public boolean isBoolean() {
        return name.equals("boolean");
    }
    public boolean isInt() {
        return name.equals("int");
    }
    public boolean isChar() {
        return name.equals("char");
    }
    public boolean isArray() {
        return name.contains("[]");
    }
    public boolean isString() {
        return name.equals("string");
    }
    public boolean isNull() {
        return name.equals("null");
    }
    public boolean isVoid() {
        return name.equals("void");
    }

    public boolean isEqual(MiniJavaType other) {
        if (this.name.equals(other.name)) return true;
        if (this.isNull() && other.isArray()) return true;
        if (this.isArray() && other.isNull()) return true;
        return false;
    }

    public boolean isPrimitive() {
        return isBoolean() || isInt() || isChar() || isString() || isNull();
    }

        private static final Map<String, Integer> PRIORITY_MAP = Map.of(
            "string", 4,
            "int", 3,
            "char", 2,
            "boolean", 1
    );


    private static final Comparator<String> PRIORITY_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String a, String b) {
            return Integer.compare(PRIORITY_MAP.get(a), PRIORITY_MAP.get(b));
        }
    };

    public boolean canExplicitCastTo(MiniJavaType other) {
        if (this.isEqual(other)) return true;
        if (this.isNull() && other.isArray()) return true;
        if (this.isPrimitive() && other.isString()) return true;
        if (this.isInt() && other.isChar()) return true;
        if (this.isChar() && other.isInt()) return true;
        return false;
    }

    public boolean canImplicitlyCastTo(MiniJavaType toType) {
        if (this.isEqual(toType)) return true;
        if (this.isChar() && toType.isInt()) return true;
        if (this.isNull() && toType.isArray()) return true;
        return false;
    }

    public static MiniJavaType maxType(MiniJavaType type1, MiniJavaType type2) {
        var ret = Collections.max(Arrays.asList(type1.name, type2.name), PRIORITY_COMPARATOR);
        return new MiniJavaType(ret);
    }

    public String toString() {
        return name;
    }
}