// Issue 411 & 547 
// Instance variable init through method invocation

class M {
    B b = new C();
}

public class Z extends M {
    
    String dd = new D().getMsg();
    String cc = b.getMsg();


}

class A{
    Z z;
    void foo() {
        z = new Z();/*<<<<<19,12,19,18,callee,B:getMsg()QString;##C:getMsg()QString;##D:D()V##D:getMsg()QString;##M:M()V*/
    }

}

class B{
    String getMsg() {
        return "hello from B";
    }
}

class C extends B{
    String getMsg() {
        return "hello from C";
    }
}

class D extends B{
    String getMsg() {
        return "hello from D";
    }
}








