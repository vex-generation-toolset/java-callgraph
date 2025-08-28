class A {
    B z = new B();

        /*<<<<<14,14,14,20,callee,B:B()V*/
    
}

class B {

}

public class Z {
    public void foo() {
        A b = new A();/*<<<<<13,4,15,4,callee,A:A()V*/
    }
}