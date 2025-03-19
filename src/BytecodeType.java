public enum BytecodeType {
    OP_CONSTANT("OP_CONSTANT"),

    OP_NIL("OP_NIL"),
    OP_TRUE("OP_TRUE"),
    OP_FALSE("OP_FALSE"),
    
    OP_FUNCTION("OP_FUNCTION"),

    OP_POP("OP_POP"),

    OP_GET_LOCAL("OP_GET_LOCAL"),
    OP_SET_LOCAL("OP_SET_LOCAL"),

    OP_GET_GLOBAL("OP_GET_GLOBAL"),
    OP_SET_GLOBAL("OP_SET_GLOBAL"),
    OP_DEFINE_GLOBAL("OP_DEFINE_GLOBAL"),

    // Pop the top element, inc/dec and push back 2 value(the same).
    OP_PRE_INC("OP_PRE_INC"),
    OP_PRE_DEC("OP_PRE_DEC"),
    // Get but not pop the top element, inc/dec and push back.
    OP_POST_INC("OP_POST_INC"),
    OP_POST_DEC("OP_POST_DEC"),

    OP_ADD("OP_ADD"),
    OP_SUB("OP_SUB"),
    OP_MUL("OP_MUL"),
    OP_DIV("OP_DIV"),
    OP_MOD("OP_MOD"),
    OP_LSHIFT("OP_LSHIFT"),
    OP_RSHIFT("OP_RSHIFT"),
    OP_URSHIFT("OP_URSHIFT"),
    OP_BIT_AND("OP_BIT_AND"),
    OP_BIT_OR("OP_BIT_OR"),
    OP_BIT_XOR("OP_BIT_XOR"),
    OP_BIT_NOT("OP_BIT_NOT"),
    OP_NOT("OP_NOT"),
    OP_NEG("OP_NEG"),
    OP_EQ("OP_EQ"),
    OP_NEQ("OP_NEQ"),
    OP_GE("OP_GE"),
    OP_LE("OP_LE"),
    OP_GT("OP_GT"),
    OP_LT("OP_LT"),

    OP_LABEL("OP_LABEL"),
    OP_JUMP("OP_JUMP"),
    OP_JUMP_IF_TRUE("OP_JUMP_IF_TRUE"),
    OP_JUMP_IF_FALSE("OP_JUMP_IF_FALSE"),

    OP_CLASS("OP_CLASS"),
    OP_INHERIT("OP_INHERIT"),
    OP_METHOD("OP_METHOD"),
    OP_INVOKE("OP_INVOKE"),
    OP_GET_PROPERTY("OP_GET_PROPERTY"),
    OP_SET_PROPERTY("OP_SET_PROPERTY"),
    OP_GET_SUPER("OP_GET_SUPER"),
    OP_SUPER_INVOKE("OP_SUPER_INVOKE"),

    OP_CALL("OP_CALL"),
    OP_RETURN("OP_RETURN"),

    OP_ARRAY("OP_ARRAY"),
    OP_GET_INDEX("OP_GET_INDEX"),
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