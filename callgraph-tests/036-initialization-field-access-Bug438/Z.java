// Issue 411 & 547 
// field access in field initialization.

public class Z {
    C c = new C();
    String bb = c.str;
    String aa;
    /*<<<<<15,11,15,17,callee,C:C()V*/

}

class A{
    String i = "10";
    String j = "100";
    Z zz = new Z();
    /*<<<<<22,17,22,23,callee,Z:Z()V*/
}

class B{
    A a;
    void foo() {
        this.a = new A();
    }
}

class C{
    String str;
    C(){
        this.str = "hello";
    }
}







