// Issue 52

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

/*$$$$$ DemoClass.foo(),
  2,
  Point#Point(int@@@int),
  Point#x()
*/
