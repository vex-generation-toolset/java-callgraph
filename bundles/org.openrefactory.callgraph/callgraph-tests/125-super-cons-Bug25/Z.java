// Issue 25
// Subclass of a Concrete Library Class Calling a Protected Library Method

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;

public class MyInputStream extends FilterInputStream {
    protected MyInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int b = super.read();  // calls FilterInputStream.read()
        return (b == -1) ? b : Character.toUpperCase(b);
    }
}

/*$$$$$
  MyInputStream#read(), 2,
  java.io.FilterInputStream#read(),
  java.lang.Character.toUpperCase(int)
*/

/*$$$$$
 MyInputStream#MyInputStream(InputStream), 2,
 MyInputStream.<init>(),
 java.io.FilterInputStream#FilterInputStream(InputStream)
 */
