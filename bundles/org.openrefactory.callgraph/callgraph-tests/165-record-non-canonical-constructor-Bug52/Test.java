// Issue 52
// Test for non-canonical constructor of record

record Point(int x, int y) {
    public Point(int v) {
        this(v, v);
    }
}

public class Demo {
    public static void foo() {
        Point p = new Point(1);
    }
}

/*$$$$$ Demo.foo(),
  1,
  Point#Point(int)
*/
