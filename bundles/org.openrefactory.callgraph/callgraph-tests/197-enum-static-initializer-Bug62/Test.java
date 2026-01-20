// Issue 62
// Test for enum with static initializer block

import java.util.HashMap;
import java.util.Map;

public enum Registry {
    INSTANCE;

    private static final Map<String, String> map = new HashMap<>();

    static {
        map.put("key", "value");
        initialize();
    }

    private static void initialize() {
        // ...
    }

    public void access() {
        initialize();
    }
}

/*$$$$$ Registry#access(), 1,
22,9, 1, Registry.initialize()
*/
