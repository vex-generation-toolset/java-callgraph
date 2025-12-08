// Issue 52
// Test for static members of record

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

/*$$$$$ DemoClass.foo(), 1,
  		11,9, 1, Point#print()
*/
