/**
 * This class represents a type in the MiniJava language.
 * Simply put, it is a wrapper around a string that represents the type.
 * It provides methods to check the type and to compare types.
 * It also provides methods to check if a type can be cast to another type.
 */
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
        return false;
    }

    public boolean isPrimitive() {
        return isBoolean() || isInt() || isChar() || isString() || isNull();
    }

    // `int` can be explicitly cast to `char`
    // all primitive types can be explicitly cast to `string`
    public boolean canExplicitCastTo(MiniJavaType other) {
        if (this.canImplicitlyCastTo(other)) return true;
        if (this.isPrimitive() && other.isString()) return true;
        if (this.isInt() && other.isChar()) return true;
        return false;
    }

    // Only `char` can be implicitly cast to `int`
    // Only `null` can be implicitly cast to `array[]`
    public boolean canImplicitlyCastTo(MiniJavaType toType) {
        if (this.isEqual(toType)) return true;
        if (this.isChar() && toType.isInt()) return true;
        if (this.isNull() && toType.isArray()) return true;
        return false;
    }

    public String toString() {
        return name;
    }
}