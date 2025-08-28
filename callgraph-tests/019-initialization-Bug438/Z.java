// Issue 411 
// Instance variables calling constructors in chains
class M {
    public M() {
        
    }
}
public class Z {
    C c = new C();
    String aa;
    String bb = new M();
}

class A{
    String i = "10";
    String j = "100";
    Z zz = new Z();
}

class B{
    A a;
    void foo() {
        this.a = new A();/*<<<<<23,17,23,23,callee,Z:Z()V*/
    }
}

class C{
    String str;
    C(){
        this.str = "hello";
        Z z = new Z();/*<<<<<31,14,31,20,callee,M:M()V,C:C()V*/
    }
}








