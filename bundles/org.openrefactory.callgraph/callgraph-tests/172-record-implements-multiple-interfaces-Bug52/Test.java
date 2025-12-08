// Issue 52
// Test for record implementing multiple interfaces

interface A {
    void a();
}

interface B {
    void b();
}

record RB() implements A, B {
    @Override
    public void a() {}

    @Override
    public void b() {}
}

public class Demo {
    public static void f() {
        A a = new RB();
        B b = new RB();
        RB rb = new RB();

        a.a();
        b.b();
        rb.a();
        rb.b();
    }
}

/*$$$$$ Demo.f(),
  4,
  RB#a(),
  RB#b(),
  RB.<init>(),
  RB#RB()
*/
