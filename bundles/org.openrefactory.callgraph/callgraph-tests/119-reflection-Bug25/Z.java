// Issue 25
// Reflection

import java.lang.reflect.Constructor;
import java.util.Date;

public class Example9 {
    void demo() throws Exception {
        Constructor<Date> c = Date.class.getConstructor();
        Date d = c.newInstance();
    }
}

/*$$$$$
  Example9#demo(), 2,
  java.lang.Class#getConstructor(),
  java.lang.reflect.Constructor#newInstance()
*/

/*!!!!! Example9#demo(), 2, 178, 27, 224, 15 */
