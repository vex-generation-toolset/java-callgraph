// Issue 25
// Library constructor

import java.util.Date;

public class Example1 {
    void demo() {
        Date now = new Date();
    }
}


/*$$$$$
  Example1#demo(), 2,
  java.util.Date#Date(),
  java.util.Date.<init>()
*/

/*!!!!!
Example1#demo(), 1,
121, 10
*/
