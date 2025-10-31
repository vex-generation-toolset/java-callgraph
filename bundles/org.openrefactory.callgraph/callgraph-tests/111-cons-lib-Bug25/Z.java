// Issue 25
// Class with Checked Exceptions on Construction

import java.io.FileReader;
import java.io.IOException;

public class Example8 {
    void demo() throws IOException {
        FileReader reader = new FileReader("/tmp/test.txt");  // may throw IOException
    }
}

/*$$$$$
  Example8#demo(), 1,
  java.io.FileReader#FileReader(String),
*/
