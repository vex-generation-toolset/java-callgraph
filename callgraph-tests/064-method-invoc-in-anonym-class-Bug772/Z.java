
interface A {
    public Z bar();/*<<<<<12,4,20,4,callee,ANON__OR__TYPE$A:ANON__OR__TYPE$A()V##Z:bazz(QA;)QA;*/
}

public class Z {
    A a;
    
    A bazz(A a) {
        return a;
    }
    Z foo() {
        a = bazz(new A() {
            @Override
            public Z bar() {
                Z z = new Z();
                return z.foo();
            }
        });
    }
}


// Issue 1374
// Previously calling interface default ethod. Not doing it now since interface does not have constructors.
