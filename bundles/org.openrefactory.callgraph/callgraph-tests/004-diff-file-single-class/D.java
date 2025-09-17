public class D extends A {
    A foo(A x) {
        A b = new B();
        b.foo(x);
        return new A();
    }
}