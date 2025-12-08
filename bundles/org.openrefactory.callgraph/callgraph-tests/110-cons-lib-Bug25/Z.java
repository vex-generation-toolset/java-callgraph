// Issue 25
// Abstract Superclass with Concrete Subclass

import java.util.AbstractList;
import java.util.ArrayList;

public class Example7 {
    void demo() {
        AbstractList<String> list = new ArrayList<>();  // instantiate subclass
    }
}

/*$$$$$ Example7#demo(), 1,
  		9,37, 2, java.util.ArrayList#ArrayList(), java.util.ArrayList.<init>(),
*/
