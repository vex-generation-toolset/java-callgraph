// Issue 52

public class DemoClass {
    public static void foo() {
        record Point(int x, int y) {}
        Point p = new Point(1, 2);
        p.x();
    }
}

/*$$ DemoClass.foo(),
  2,
  Point#Point(int@@@int),
  DemoClass.foo().Point#x()
*/

// This test is currently disabled because it fails in extended callgraph test suite run.
// NB: it runs correctly inside projects.
