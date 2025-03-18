import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public enum MiniJavaType {
    STRING("string", 5),
    INT("int", 4),
    CHAR("char", 3),
    BOOLEAN("boolean", 2),
    NULL("null", 1),
    CONSTANT("constant", 0);

    private final String name;
    private final int priority;
    MiniJavaType(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    @Override
    public String toString() {
        return name;
    }

    public static final Comparator<MiniJavaType> PRIORITY_COMPARATOR =
            Comparator.comparingInt(pt -> pt.priority);

    public boolean isString() { return this == STRING; }
    public boolean isInt() { return this == INT; }
    public boolean isChar() { return this == CHAR; }
    public boolean isBoolean() { return this == BOOLEAN; }
    public boolean isNull() { return this == NULL; }

    public static MiniJavaType maxType(MiniJavaType type1, MiniJavaType type2) {
        return Collections.max(Arrays.asList(type1, type2), MiniJavaType.PRIORITY_COMPARATOR);
    }
}
