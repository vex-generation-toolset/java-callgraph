// Issue 411 
// Instance variables calling constructors in chains

public class Z {
    String aa;    
    String bb = new String();
    C c = new C();
}

class A{
    Z z;
    void foo() {
        z = new Z();
    }
}

class C{
    String str;
    C(){
        this.str="hello";/*<<<<<13,12,13,18,callee,C:C()V*/
    }
}






