
interface A {
    public Z bar();
}

public class Z {
    A a;
    
    A bazz(A a) {
        return a;
    }
    void foo() {
        a = bazz(new A() {
            @Override
            public Z bar() {
                return new Z();
            }
        });/*<<<<<12,4,19,4,callee, ANON__OR__TYPE$A:ANON__OR__TYPE$A()V##Z:bazz(QA;)QA;*/
    }
}
