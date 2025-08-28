// Issue 411 
// Instance variable init through constructor call

class M {
    String mm;
    public M() {
        mm = "mm";
    }
}

public class Z {
    String aa;
    String bb = new M();
}

class A{
    Z z;
    void foo() {
        z = new Z();/*<<<<<19,12,19,18,callee,M:M()V*/
    }
}





