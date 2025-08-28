// Issue 411 
// Instance variable init through method invocation

class M {
    B b = new C();
    String foo() {
        return "hehe";
    }
}

public class Z extends M {
    String dd = new D().getMsg();
    String cc = b.mm.foo();

}

class A{
    Z z;
    void foo() {
        z = new Z();/*<<<<<20,12,20,18,callee,D:D()V,D:getMsg()QString;,M:foo()QString;,M:M()V*/
    }

}

class B{
    public M mm;
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

// Issue 547






