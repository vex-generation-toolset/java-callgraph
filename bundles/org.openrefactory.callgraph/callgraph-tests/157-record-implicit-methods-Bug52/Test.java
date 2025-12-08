// Issue 52
// Test for implicit methods of record

record Point(int x, int y) {}

public class DemoClass {
    public static void foo() {
        Point p = new Point(1, 2);
        p.x();
        p.y();
        p.toString();
        p.hashCode();
        p.equals(p);
    }
}

/*$$$$$ DemoClass.foo(), 6,
		8,19, 1, Point#Point(int@@@int),
		9,9, 1, Point#x(),
		10,9, 1, Point#y(),
		11,9, 1, Point#toString(),
		12,9, 1, Point#hashCode(),
		13,9, 1, Point#equals(Point)
*/
