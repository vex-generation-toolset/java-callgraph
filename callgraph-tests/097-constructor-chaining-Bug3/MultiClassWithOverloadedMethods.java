// Issue 3
// Demonstrates the chaining of default constructor to super class.
// The default constructor of B, C, D will call the default constructor of A

import java.util.ArrayList;

class A {
    A foo(A x) {
        return x;
    }
}

class B extends A {
    A foo(A x) {
        return new D();
    }
}

class D extends A {
    A foo(A x) {
        A b = new B();
        b.foo(x);
        return new A();
    }
    
    A foo(A x, ArrayList<A> y) {
        return y;
    }
}

class C extends A {
    
    A foo(A x) {
        D y = new D();
        y.foo(x);
        return this;
    }
}


public class MultiClassWithOverloadedMethods {
    public static void main(String[] args) {
        A x = new A();
        int i = 10;
        while(i>0) {
            x = x.foo(new B());
            i--;
        }
        A y = new C();
        y.foo(x);
    }
}

/*$$$$$ A.<init>(), 0 */

/*$$$$$ B.<init>(), 1, A.<init>() */

/*$$$$$ C.<init>(), 1, A.<init>() */

/*$$$$$ D.<init>(), 1, A.<init>() */

