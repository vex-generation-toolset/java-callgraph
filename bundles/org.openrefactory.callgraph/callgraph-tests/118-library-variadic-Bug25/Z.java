// Issue 25
// Using a Static Method Returning an Instance

import java.util.List;

public class Example7 {
    void demo() {
        List<String> list = List.of("a", "b", "c");
    }
}

/*$$$$$
  Example7#demo(), 1,
  java.util.List.of(String@@@String@@@String)
*/
