import java.util.Stack;

public class VM {
    private final Environment environment;
    private final BytecodeGenerator bytecodeGenerator;

    private final Stack<MiniJavaObject> stack = new Stack<>();

    private String scope = "0_global";
    // TODO: Implement the program counter, maynot start from 0.
    private Integer pc = 0;

    public VM(Environment environment, BytecodeGenerator bytecodeGenerator) {
        this.environment = environment;
        this.bytecodeGenerator = bytecodeGenerator;
    }

    public void run() {
        Integer bytecode_size = bytecodeGenerator.bytecodes.size();
        while (true) {
            if (pc >= bytecode_size) {
                break;
            }
            var bytecode = bytecodeGenerator.bytecodes.get(pc);
            runBytecode(bytecode);
        }
    }

    private void runBytecode(Bytecode bytecode) {
        switch (bytecode.type) {
            case OP_CONSTANT:
                var constant = environment.pools.get(scope).constantPool.get(bytecode.arg1);
                stack.push(constant);
                pc += 1;
                return;
            case OP_NIL:
                stack.push(new MiniJavaObject(MiniJavaType.NULL, null));
                pc += 1;
                return;
            case OP_TRUE:
                stack.push(new MiniJavaObject(MiniJavaType.BOOLEAN, true));
                pc += 1;
                return;
            case OP_FALSE:
                stack.push(new MiniJavaObject(MiniJavaType.BOOLEAN, false));
                pc += 1;
                return;
            case OP_POP:
                stack.pop();
                pc += 1;
                return;
            case OP_GET_LOCAL:
            case OP_SET_LOCAL:
                runLocal(bytecode);
                pc += 1;
                return;
            case OP_GET_GLOBAL:
            case OP_SET_GLOBAL:
                runGlobal(bytecode);
                pc += 1;
                return;
            case OP_PRE_INC:
            case OP_PRE_DEC:
            case OP_POST_INC:
            case OP_POST_DEC:
            case OP_NEG:
            case OP_BIT_NOT:
                runUnary(bytecode);
                pc += 1;
                return;
            case OP_ADD:
            case OP_SUB:
            case OP_MUL:
            case OP_DIV:
            case OP_MOD:
            case OP_LSHIFT:
            case OP_RSHIFT:
            case OP_URSHIFT:
            case OP_BIT_AND:
            case OP_BIT_OR:
            case OP_BIT_XOR:
            case OP_EQ:
            case OP_NEQ:
            case OP_GE:
            case OP_LE:
            case OP_GT:
            case OP_LT:
                runBinary(bytecode);
                pc += 1;
                return;
            case OP_LABEL:
                pc += 1;
                return;
            case OP_JUMP:
            case OP_JUMP_IF_TRUE:
            case OP_JUMP_IF_FALSE:
                runJump(bytecode);
                return;
            default:
                throw new RuntimeException("Unknown bytecode: " + bytecode.type);
            
        }
    }

    private void runLocal(Bytecode bytecode) {
        var variable = environment.pools.get(scope).variablePool.get(bytecode.arg1);
        if (bytecode.type == BytecodeType.OP_GET_LOCAL) {
            stack.push(variable);
        } else {
            variable.value = stack.peek().value;
        }
    }

    private void runGlobal(Bytecode bytecode) {
        var variable = environment.pools.get("0_global").variablePool.get(bytecode.arg1);
        if (bytecode.type == BytecodeType.OP_GET_GLOBAL) {
            stack.push(variable);
        } else {
            variable.value = stack.peek().value;
        }
    }

    private void runUnary(Bytecode bytecode) {
        // Special case for post inc/dec
        if (bytecode.type == BytecodeType.OP_POST_INC || bytecode.type == BytecodeType.OP_POST_DEC) {
            var operand = (int) stack.peek().value;
            if (bytecode.type == BytecodeType.OP_POST_INC) {
                stack.push(new MiniJavaObject(MiniJavaType.INT, operand + 1));
            } else {
                stack.push(new MiniJavaObject(MiniJavaType.INT, operand - 1));
            }
            return;
        }

        var operand = (int) stack.pop().value;
        switch (bytecode.type) {
            case OP_PRE_INC:
                stack.push(new MiniJavaObject(MiniJavaType.INT, operand + 1));
                stack.push(new MiniJavaObject(MiniJavaType.INT, operand + 1));
                return;
            case OP_PRE_DEC:
                stack.push(new MiniJavaObject(MiniJavaType.INT, operand - 1));
                stack.push(new MiniJavaObject(MiniJavaType.INT, operand - 1));
                return;
            case OP_NEG:
                stack.push(new MiniJavaObject(MiniJavaType.INT, -operand));
                return;
            case OP_BIT_NOT:
                stack.push(new MiniJavaObject(MiniJavaType.INT, ~operand));
                return;
            default:
                throw new RuntimeException("Unknown unary operator: " + bytecode.type);
        }
    }

    private void runBinary(Bytecode bytecode) {
        var left = (int) stack.pop().value;
        var right = (int) stack.pop().value;
        switch (bytecode.type) {
            case OP_ADD:
                stack.push(new MiniJavaObject(MiniJavaType.INT, left + right));
                return;
            case OP_SUB:
                stack.push(new MiniJavaObject(MiniJavaType.INT, left - right));
                return;
            case OP_MUL:
                stack.push(new MiniJavaObject(MiniJavaType.INT, left * right));
                return;
            case OP_DIV:
                stack.push(new MiniJavaObject(MiniJavaType.INT, left / right));
                return;
            case OP_MOD:
                stack.push(new MiniJavaObject(MiniJavaType.INT, left % right));
                return;
            case OP_LSHIFT:
                stack.push(new MiniJavaObject(MiniJavaType.INT, left << right));
                return;
            case OP_RSHIFT:
                stack.push(new MiniJavaObject(MiniJavaType.INT, left >> right));
                return;
            case OP_URSHIFT:
                stack.push(new MiniJavaObject(MiniJavaType.INT, left >>> right));
                return;
            case OP_BIT_AND:
                stack.push(new MiniJavaObject(MiniJavaType.INT, left & right));
                return;
            case OP_BIT_OR:
                stack.push(new MiniJavaObject(MiniJavaType.INT, left | right));
                return;
            case OP_BIT_XOR:
                stack.push(new MiniJavaObject(MiniJavaType.INT, left ^ right));
                return;
            case OP_EQ:
                stack.push(new MiniJavaObject(MiniJavaType.BOOLEAN, left == right));
                return;
            case OP_NEQ:
                stack.push(new MiniJavaObject(MiniJavaType.BOOLEAN, left != right));
                return;
            case OP_GE:
                stack.push(new MiniJavaObject(MiniJavaType.BOOLEAN, left >= right));
                return;
            case OP_LE:
                stack.push(new MiniJavaObject(MiniJavaType.BOOLEAN, left <= right));
                return;
            case OP_GT:
                stack.push(new MiniJavaObject(MiniJavaType.BOOLEAN, left > right));
                return;
            case OP_LT:
                stack.push(new MiniJavaObject(MiniJavaType.BOOLEAN, left < right));
                return;
            default:
                throw new RuntimeException("Unknown binary operator: " + bytecode.type);
        }
    }

    private Integer searchJumpTarget(Integer label) {
        for (int i = 0; i < bytecodeGenerator.bytecodes.size(); i++) {
            var current = bytecodeGenerator.bytecodes.get(i);
            if (current.type == BytecodeType.OP_LABEL && current.arg1 == label) {
                return i;
            }
        }
        throw new RuntimeException("Cannot find label: " + label);
    }

    private void runJump(Bytecode bytecode) {
        var label = bytecode.arg1;
        var target = pc + 1;
        if (bytecode.type == BytecodeType.OP_JUMP) {
            target = searchJumpTarget(label);
        } else {
            var condition = (boolean) stack.pop().value;
            if ((bytecode.type == BytecodeType.OP_JUMP_IF_TRUE && condition) ||
                (bytecode.type == BytecodeType.OP_JUMP_IF_FALSE && !condition)) {
                target = searchJumpTarget(label);
            }
        }
        pc = target;
    }
}
