// Issue 52

record Point(int x, int y) {
    static int zero = 0;
    static void print() {}
}

public class DemoClass {
    public static void foo() {
        Point.print();
        int z = Point.zero;
    }
}

/*$$$$$ DemoClass.foo(),
  1,
  Point#print()
*/
