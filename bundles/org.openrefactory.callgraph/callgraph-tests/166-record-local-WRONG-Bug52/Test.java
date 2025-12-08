// Issue 52
// Test for local record

public class DemoClass {
    public static void foo() {
        record Point(int x, int y) {}
        Point p = new Point(1, 2);
        p.x();
    }
}

/*$$ DemoClass.foo(), 2,
		7,19, 1, Point#Point(int@@@int),
		8,9, 1, DemoClass.foo().Point#x()
*/

// This test is currently disabled because it fails in extended callgraph test suite run.
// NB: it runs correctly inside projects.
