class A {
    void bar() {
        
    }
}

class B extends A {
    void bazz() {
        
    }
}

class C extends B {
    void bazz() {
        
    }
}

public class Z {
    void foo() {
        B b = new B();
        b.bar();/*<<<<<20,4,23,4,callee,A:bar()V,B:B()V*/
    }
}