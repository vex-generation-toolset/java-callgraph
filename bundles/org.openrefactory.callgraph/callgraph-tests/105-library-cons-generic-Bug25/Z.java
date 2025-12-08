// Issue 25
// Library constructor parameterized

import java.util.HashMap;

public class Example3 {
    void demo() {
        HashMap<String, Integer> ages = new HashMap<>(100);
    }
}


/*$$$$$ Example3#demo(), 1,
  		8,41, 1, java.util.HashMap#HashMap(int),
*/
