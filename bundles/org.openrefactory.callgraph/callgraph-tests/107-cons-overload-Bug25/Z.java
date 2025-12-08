// Issue 25
// Two arg constructor

import java.io.File;

public class Example5 {
    void demo() {
        File f1 = new File("/tmp/test.txt");            // single-arg constructor
        File f2 = new File("/tmp", "test.txt");         // two-arg constructor
    }
}

/*$$$$$ Example5#demo(), 2,
  		8,19, 1, java.io.File#File(String),
  		9,19, 1, java.io.File#File(String@@@String),
*/
