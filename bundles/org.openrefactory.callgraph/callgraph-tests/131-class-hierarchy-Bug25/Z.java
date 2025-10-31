// Issue 25
// Multiple Layers of Inheritance (Library in the Middle)

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

class Middle extends FilterOutputStream {
    protected Middle(OutputStream out) { super(out); }
    @Override
    public void write(int b) throws IOException {
        System.out.println("Middle writing");
        super.write(b);  // calls FilterOutputStream.write()
    }
}

public class MyOutputStream extends Middle {
    public MyOutputStream(OutputStream out) { super(out); }

    @Override
    public void write(int b) throws IOException {
        System.out.println("Top layer writing");
        super.write(b);  // still resolves eventually to FilterOutputStream.write()
    }
}

/*$$$$$
  MyOutputStream#MyOutputStream(OutputStream), 2,
  Middle#Middle(OutputStream),
  MyOutputStream.<init>()
*/


/*$$$$$
  MyOutputStream#write(int), 1,
  Middle#write(int),
*/

/*$$$$$
  Middle#Middle(OutputStream), 2,
  java.io.FilterOutputStream#FilterOutputStream(OutputStream),
  Middle.<init>(),
*/

/*$$$$$
  Middle#write(int), 1,
  java.io.FilterOutputStream#write(int),
*/

