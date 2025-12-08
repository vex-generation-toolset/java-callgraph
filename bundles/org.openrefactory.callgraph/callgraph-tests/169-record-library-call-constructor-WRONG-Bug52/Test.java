// Issue 52
// Test for record with non-canonical constructor that calls a library method

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

/*$$$$$ Demo.foo(), 1,
  		14,19, 1, Point#Point(int@@@int)
*/

/*$$ Point#Point(int@@@int), 1,
		8,9, 1, java.util.Objects#requireNonNull(int)
*/

// This test is currently disabled because it fails in extended callgraph test suite run.
// NB: it runs correctly inside projects.
