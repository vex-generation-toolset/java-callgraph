// Issue 52
// Test for explicit accessor method of record

record Point(int x, int y) {
    public int x() {
        return x;
    }
}

public class DemoClass {
    public static void foo() {
        Point p = new Point(1, 2);
        p.x();
    }
}

/*$$$$$ DemoClass.foo(), 2,
  		12,19, 1, Point#Point(int@@@int),
  		13,9, 1, Point#x()
*/
