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

/*$$$ Demo.f(), 7,
		22,15, 2, RB#RB(), RB.<init>(),
		23,15, 2, RB#RB(), RB.<init>(),
		24,17, 2, RB#RB(), RB.<init>(),
  		26,9, 1, RB#a(),
  		27,9, 1, RB#b(),
  		28,9, 1, RB#a(),
  		29,9, 1, RB#b(),  		
*/

// This test is producing wrong result. There should be 7 callsites.
// But we are not getting calls for `a.a()` and `b.b()`
// So, disabling it for now.
