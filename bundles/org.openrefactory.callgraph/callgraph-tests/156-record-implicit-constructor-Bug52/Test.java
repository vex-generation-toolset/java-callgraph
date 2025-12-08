// Issue 52
// Test for implicit constructor of record

record Point(int x, int y) {}

public class DemoClass {
    public static void foo() {
        Point p = new Point(1, 2);
    }
}

/*$$$$$ DemoClass.foo(), 1,
		8,19, 1, Point#Point(int@@@int)
*/
