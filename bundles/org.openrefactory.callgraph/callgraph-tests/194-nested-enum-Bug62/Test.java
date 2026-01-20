// Issue 62
// Test for nested enum

public class Outer {
    public enum Direction {
        NORTH, SOUTH;

        public void print() {
            Math.sqrt(16.0);
        }
    }

    public void test() {
        Direction d = Direction.NORTH;
        d.print();
    }
}

/*$$$$$ Outer#test(), 2,
13,5, 1, Outer.Direction.<staticinit>(),
15,9, 1, Outer.Direction#print()
*/

/*$$$$$ Outer.Direction#print(), 1,
9,13, 1, java.lang.Math.sqrt(double)
*/
