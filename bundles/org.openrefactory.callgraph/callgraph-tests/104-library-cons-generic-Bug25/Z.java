// Issue 25
// Library constructor parameterized

import java.util.ArrayList;

public class Example2 {
    void demo() {
        ArrayList<String> names = new ArrayList<>();
    }
}


/*$$$$$
  Example2#demo(), 2,
  java.util.ArrayList#ArrayList(),
  java.util.ArrayList.<init>()
*/

/*!!!!! Example2#demo(), 1, 155, 17 */
