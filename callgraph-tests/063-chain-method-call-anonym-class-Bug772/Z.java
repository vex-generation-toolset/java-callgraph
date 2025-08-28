
interface A {
    public Z bar();/*<<<<<12,4,20,4,callee,A1:bar()QZ;##ANON__OR__TYPE$A:ANON__OR__TYPE$A()V##ANON__OR__TYPE$A:bar()QZ;##Z:bazz(QA;)QA;*/
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
        }).bar();
    }
}

class A1 implements A {
    @Override
    public Z bar() {
        return new Z();
    }
}

