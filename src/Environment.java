import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// this class stores the whole environment for bytecode
// specifically we create a `symbolTableStack` for each function,
// and we have a global scope named `0_global`.
// Because IDENTIFIER must start with letter not digit,
// this global name would not conflict with other function name.
public class Environment {
    public final Map<String, ArrayList<Map<String, MiniJavaType>>> environment;

    public Environment() {
        environment = new HashMap<>();
    }
}
