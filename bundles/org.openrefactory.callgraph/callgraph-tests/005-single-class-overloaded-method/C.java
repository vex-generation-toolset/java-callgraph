
public class C extends A {
    A foo(A x) {
        D d = new D();
        int i = 1;
        d.foo(x, i);
        return this;
    }
}