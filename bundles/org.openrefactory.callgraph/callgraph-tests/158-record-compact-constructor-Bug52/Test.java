// Issue 52
// Test for compact constructor of record

record Point(int x, int y) {
    public Point {
        if (x < 0) x = 0;
        if (y < 0) y = 0;
    }
}

public class DemoClass {
    public static void foo() {
        Point p = new Point(-1, -2);
    }
}

/*$$$$$ DemoClass.foo(), 1,
		13,19, 1, Point#Point(int@@@int)
*/
