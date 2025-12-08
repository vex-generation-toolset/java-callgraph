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

/*$$$$$ DemoClass.foo(),
  6,
  Point#Point(int@@@int),
  Point#x(),
  Point#y(),
  Point#toString(),
  Point#hashCode(),
  Point#equals(Point)
*/
