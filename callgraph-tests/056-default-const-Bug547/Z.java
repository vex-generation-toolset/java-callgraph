class A {
    Z z = new Z();
}

class B extends A {
    public B(int a) {/*<<<<<6,4,8,4,callee,A:A()V##B:B()V*/
        
    }
}

public class Z {
    public void foo() {
        A b = new B(2);/*<<<<<12,4,14,4,callee,B:B(I)V*/
    }
}