// Issue 52

import java.util.Objects;

record Point(int x, int y) {
    public Point {
        Objects.requireNonNull(x);
    }
}

public class Demo {
    public static void foo() {
        Point p = new Point(1, 2);
    }
}

/*$$$$$ Demo.foo(),
  1,
  Point#Point(int@@@int)
*/

/*$$ Point#Point(int@@@int),
  1,
  java.util.Objects#requireNonNull(java.lang.Object)
*/

// This test is currently disabled because it fails in extended callgraph test suite run.
// NB: it runs correctly inside projects.
