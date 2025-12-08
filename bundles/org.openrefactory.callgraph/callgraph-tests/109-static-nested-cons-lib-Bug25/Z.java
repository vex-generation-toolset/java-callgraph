// Issue 25
// Static inner class

import java.util.Map;
import java.util.HashMap;

public class Example6 {
    void demo() {
        Map.Entry<String, Integer> entry = new HashMap.SimpleEntry<>("A", 1);
    }

    void demo1() {
        Map.Entry<String, Integer> entry = new HashMap.SimpleEntry<String, Integer>("A", 1);
    }
}

// Issue 1932
// The explicit binding specification is a problem thay we got after we migrated to JLS 21 (Issue 1851).
// But for library the binding is not an issue, since we do not rely on the binding.
// So, we get the result for both <> with and without parameters.

/*$$$$$ Example6#demo(), 1,
		9,44, 1, java.util.HashMap.SimpleEntry#SimpleEntry(String@@@int)
*/


/*$$$$$ Example6#demo1(), 1,
		13,44, 1, java.util.HashMap.SimpleEntry#SimpleEntry(String@@@int)
*/
