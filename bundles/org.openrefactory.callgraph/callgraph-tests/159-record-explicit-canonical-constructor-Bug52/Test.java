// Issue 52

record Point(int x, int y) {
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

public class DemoClass {
    public static void foo() {
        Point p = new Point(1, 2);
    }
}

/*$$$$$ DemoClass.foo(),
  1,
  Point#Point(int@@@int)
*/
