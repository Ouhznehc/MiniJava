import java.util.Objects;

/**
 * This class represents a type in the MiniJava language.
 * Simply put, it is a wrapper around a string that represents the type.
 * It provides methods to check the type and to compare types.
 * It also provides methods to check if a type can be cast to another type.
 */
public class MiniJavaType {
    public String primitiveType;
    public String classType;
    public Integer arrayDimension;
    // This is a special field that is used to pass semantic checks.
    // When we invoke a class method, due to polymorphism,
    // we only know the return type at runtime.
    // So we use this field to represent the return type of a class method to pass semantics checks.
    public boolean isAnyType = false;

    public MiniJavaType(String primitiveType, String classType, Integer arrayDimension) {
        this.primitiveType = primitiveType;
        this.classType = classType;
        this.arrayDimension = arrayDimension;
    }

    public MiniJavaType(MiniJavaType type) {
        this.primitiveType = type.primitiveType;
        this.classType = type.classType;
        this.arrayDimension = type.arrayDimension;
        this.isAnyType = type.isAnyType;
    }

    public static MiniJavaType newAnyType() {
        MiniJavaType type = new MiniJavaType(null, null, 0);
        type.isAnyType = true;
        return type;
    }

    public static MiniJavaType newPrimitiveType(String name) {
        return new MiniJavaType(name, null, 0);
    }
    public static MiniJavaType newClassType(String name) {
        return new MiniJavaType(null, name, 0);
    }
    public static MiniJavaType newPrimitiveArrayType(String name, Integer dimension) {
        return new MiniJavaType(name, null, dimension);
    }
    public static MiniJavaType newClassArrayType(String name, Integer dimension) {
        return new MiniJavaType(null, name, dimension);
    }

    public boolean isClass() {
        return classType != null && !isArray();
    }

    public boolean isArray() {
        return arrayDimension != 0;
    }


    public boolean isBoolean() {
        return primitiveType != null && primitiveType.equals("boolean");
    }
    public boolean isInt() {
        return primitiveType != null && primitiveType.equals("int");
    }
    public boolean isChar() {
        return primitiveType != null && primitiveType.equals("char");
    }
    public boolean isString() {
        return primitiveType != null && primitiveType.equals("string");
    }
    public boolean isNull() {
        return primitiveType != null && primitiveType.equals("null");
    }
    public boolean isVoid() {
        return primitiveType != null && primitiveType.equals("void");
    }

    public boolean isEqual(MiniJavaType other) {
        if (Objects.equals(this.primitiveType, other.primitiveType)
            && Objects.equals(this.classType, other.classType)
            && Objects.equals(this.arrayDimension, other.arrayDimension)) return true;
        if (this.isAnyType || other.isAnyType) return true;
        return false;
    }

    public boolean isPrimitive() {
        return primitiveType != null && !isArray();
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
        if (isClass()) {
            return classType;
        } else if (isPrimitive()) {
            return primitiveType;
        } else {
            if (primitiveType != null) return primitiveType + "[]".repeat(arrayDimension);
            else return classType + "[]".repeat(arrayDimension);
        } 
    }
}