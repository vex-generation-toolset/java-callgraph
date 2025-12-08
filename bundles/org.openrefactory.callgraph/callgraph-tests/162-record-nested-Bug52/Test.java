// Issue 52
// Test for nested record

class Outer {
    record Inner(int x) {}
    public static void foo() {
        Inner i = new Inner(1);
        i.x();
    }
}

/*$$$$$ Outer.foo(), 2,
  		7,19, 1, Inner#Inner(int),
  		8,9, 1, Inner#x()
*/
