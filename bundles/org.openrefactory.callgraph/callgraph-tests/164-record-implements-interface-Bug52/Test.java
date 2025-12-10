// Issue 52
// Test for record implementing an interface

interface I {
    void foo();
}

record R() implements I {
    public void foo() {}
}

public class DemoClass {
    public static void f() {
        I i = new R();
        i.foo();
    }
}

/*$$$$$ DemoClass.f(),
  3,
  R.<init>(),
  R#R(),
  I#foo()
*/
