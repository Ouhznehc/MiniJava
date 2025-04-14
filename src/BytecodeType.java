public enum BytecodeType {
    // Find constant value in the constant pool with `constant_index`,
    // and load it onto the stack.
    // Stack before: []
    // Stack after: [constant]
    // Usage: OP_CONSTANT <constant_index>
    OP_CONSTANT("OP_CONSTANT"),

    // Pushes a nil (null) value onto the stack.
    // Stack before: []
    // Stack after: [nil]
    // Usage: OP_NIL
    OP_NIL("OP_NIL"),

    // Pushes a boolean true onto the stack.
    // Stack before: []
    // Stack after: [true]
    // Usage: OP_TRUE
    OP_TRUE("OP_TRUE"),

    // Pushes a boolean false onto the stack.
    // Stack before: []
    // Stack after: [false]
    // Usage: OP_FALSE
    OP_FALSE("OP_FALSE"),

    // Pops the top value from the stack.
    // Stack before: [value]
    // Stack after: []
    // Usage: OP_POP
    OP_POP("OP_POP"),

    // Find a variable in the variable pool with `variable_index`
    // and load this variable onto the stack.
    // Stack before: []
    // Stack after: [variable]
    // Usage: OP_GET_LOCAL <variable_index>
    OP_GET_LOCAL("OP_GET_LOCAL"),

    // Find a variable in the variable pool with `variable_index`
    // and set this variable to the top value of the stack.
    // Stack before: [value]
    // Stack after: [value]
    // Usage: OP_SET_LOCAL <variable_index>
    OP_SET_LOCAL("OP_SET_LOCAL"),

    // Increments the top value of the stack.
    // Stack before: [value]
    // Stack after: [value + 1]
    // Usage: OP_INC
    OP_INC("OP_INC"),

    // Decrements the top value of the stack.
    // Stack before: [value]
    // Stack after: [value - 1]
    // Usage: OP_DEC
    OP_DEC("OP_DEC"),

    // Adds the top two values on the stack.
    // Stack before: [a, b]
    // Stack after: [a + b]
    // Usage: OP_ADD
    OP_ADD("OP_ADD"),

    // Subtracts the top value from the second-to-top value on the stack.
    // Stack before: [a, b]
    // Stack after: [a - b]
    // Usage: OP_SUB
    OP_SUB("OP_SUB"),

    // Multiplies the top two values on the stack.
    // Stack before: [a, b]
    // Stack after: [a * b]
    // Usage: OP_MUL
    OP_MUL("OP_MUL"),

    // Divides the second-to-top value by the top value on the stack.
    // Stack before: [a, b]
    // Stack after: [a / b]
    // Usage: OP_DIV
    OP_DIV("OP_DIV"),

    // Computes the modulus of the second-to-top value by the top value on the stack.
    // Stack before: [a, b]
    // Stack after: [a % b]
    // Usage: OP_MOD
    OP_MOD("OP_MOD"),

    // Performs a left bitwise shift.
    // Stack before: [a, b]
    // Stack after: [a << b]
    // Usage: OP_LSHIFT
    OP_LSHIFT("OP_LSHIFT"),

    // Performs a right bitwise shift.
    // Stack before: [a, b]
    // Stack after: [a >> b]
    // Usage: OP_RSHIFT
    OP_RSHIFT("OP_RSHIFT"),

    // Performs an unsigned right bitwise shift.
    // Stack before: [a, b]
    // Stack after: [a >>> b]
    // Usage: OP_URSHIFT
    OP_URSHIFT("OP_URSHIFT"),

    // Performs a bitwise AND operation.
    // Stack before: [a, b]
    // Stack after: [a & b]
    // Usage: OP_BIT_AND
    OP_BIT_AND("OP_BIT_AND"),

    // Performs a bitwise OR operation.
    // Stack before: [a, b]
    // Stack after: [a | b]
    // Usage: OP_BIT_OR
    OP_BIT_OR("OP_BIT_OR"),

    // Performs a bitwise XOR operation.
    // Stack before: [a, b]
    // Stack after: [a ^ b]
    // Usage: OP_BIT_XOR
    OP_BIT_XOR("OP_BIT_XOR"),

    // Performs a bitwise NOT operation.
    // Stack before: [a]
    // Stack after: [~a]
    // Usage: OP_BIT_NOT
    OP_BIT_NOT("OP_BIT_NOT"),

    // Negates the top value on the stack.
    // Stack before: [a]
    // Stack after: [-a]
    // Usage: OP_NEG
    OP_NEG("OP_NEG"),

    // Checks if the top two values on the stack are equal.
    // Stack before: [a, b]
    // Stack after: [a == b]
    // Usage: OP_EQ
    OP_EQ("OP_EQ"),

    // Checks if the top two values on the stack are not equal.
    // Stack before: [a, b]
    // Stack after: [a != b]
    // Usage: OP_NEQ
    OP_NEQ("OP_NEQ"),

    // Checks if the second-to-top value is greater than or equal to the top value.
    // Stack before: [a, b]
    // Stack after: [a >= b]
    // Usage: OP_GE
    OP_GE("OP_GE"),

    // Checks if the second-to-top value is less than or equal to the top value.
    // Stack before: [a, b]
    // Stack after: [a <= b]
    // Usage: OP_LE
    OP_LE("OP_LE"),

    // Checks if the second-to-top value is greater than the top value.
    // Stack before: [a, b]
    // Stack after: [a > b]
    // Usage: OP_GT
    OP_GT("OP_GT"),

    // Checks if the second-to-top value is less than the top value.
    // Stack before: [a, b]
    // Stack after: [a < b]
    // Usage: OP_LT
    OP_LT("OP_LT"),

    // Marks a label for jump instructions.
    // Stack before: [...]
    // Stack after: [...]
    // Usage: OP_LABEL <label>
    OP_LABEL("OP_LABEL"),

    // Unconditionally jumps to a label.
    // Stack before: [...]
    // Stack after: [...]
    // Usage: OP_JUMP <label>
    OP_JUMP("OP_JUMP"),

    // Jumps to a label if the top value is true.
    // Stack before: [..., condition]
    // Stack after: [...]
    // Usage: OP_JUMP_IF_TRUE <label>
    OP_JUMP_IF_TRUE("OP_JUMP_IF_TRUE"),

    // Jumps to a label if the top value is false.
    // Stack before: [..., condition]
    // Stack after: [...]
    // Usage: OP_JUMP_IF_FALSE <label>
    OP_JUMP_IF_FALSE("OP_JUMP_IF_FALSE"),

    // Defines a method with the `method_name`.
    // Stack before: []
    // Stack after: []
    // Usage: OP_METHOD <method_name>
    OP_METHOD("OP_METHOD"),

    // Invokes a method on an object.
    // Stack before: [..., object, argument1, argument2, ...]
    // Stack after: [..., result]
    // Usage: OP_INVOKE <method_name> <argument_count>
    OP_INVOKE("OP_INVOKE"),


    // Creates a new object of the class with `class_name`.
    // Stack before: []
    // Stack after: []
    // Usage: OP_CLASS <class_name>
    OP_CLASS("OP_CLASS"),

    // Gets a field from an object.
    // Stack before: [object]
    // Stack after: [object.property]
    // Usage: OP_GET_FIELD <filed_name>
    OP_GET_FIELD("OP_GET_FIELD"),

    // Sets a field on an object.
    // Stack before: [object, value]
    // Stack after: [value]
    // Usage: OP_SET_FIELD <filed_name>
    OP_SET_FIELD("OP_SET_FIELD"),

    // Gets the instance of current class.
    // Stack before: []
    // Stack after: [this]
    // Usage: OP_THIS
    OP_THIS("OP_THIS"),

    // Gets the super instance of the class.
    // Stack before: [object]
    // Stack after: [object.super]
    // Usage: OP_SUPER
    OP_SUPER("OP_SUPER"),

    // Cast the top value to a specific type.
    // Stack before: [value]
    // Stack after: [casted_value]
    // Usage: OP_CAST <type_index>
    OP_CAST("OP_CAST"),

    // Check the object is a instance of a specific type.
    // Stack before: [object]
    // Stack after: [true/false]
    // Usage: OP_INSTANCE_OF <type_index>
    OP_INSTANCE_OF("OP_INSTANCE_OF"),

    // Calls a function with arguments.
    // Stack before: [argument1, argument2, ..., argumentN]
    // Stack after: [result]
    // Usage: OP_CALL <method_name> <argument_count>
    OP_CALL("OP_CALL"),

    // Returns from a function.
    // Stack before: []
    // Stack after: [return_value]
    // Usage: OP_RETURN
    OP_RETURN("OP_RETURN"),

    // Creates a new array with size.
    // Stack before: [ size ]
    // Stack after: [ array ]
    // Usage: OP_NEW_ARRAY
    OP_NEW_ARRAY("OP_NEW_ARRAY"),

    // Duplicates the top value on the stack.
    // Stack before: [value]
    // Stack after: [value, value]
    // Usage: OP_DUP
    OP_DUP("OP_DUP"),

    // Gets an element from an array.
    // Stack before: [ array, index ]
    // Stack after: [ array[index] ]
    // Usage: OP_GET_INDEX
    OP_GET_INDEX("OP_GET_INDEX"),

    // Sets an element in an array.
    // Stack before: [array, index, value]
    // Stack after: [value]
    // Usage: OP_SET_INDEX
    OP_SET_INDEX("OP_SET_INDEX");

    private final String name;

    BytecodeType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}