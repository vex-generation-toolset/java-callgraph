// Issue 411 
// Instance variable init through method invocation

class M {
    B b = new C();
    String foo() {
        return "hehe";
    }
}

public class Z extends M {
    String dd;
    String cc;
    N n;
    
    {
        dd = new D().getMsg();
        cc = b.mm.foo();
        n = new N();
    }

}

class A{
    Z z;
    void foo() {
        z = new Z();/*<<<<<27,12,27,18,callee,N:N()V,D:D()V,D:getMsg()QString;,M:foo()QString;,M:M()V*/
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

class AB {
    
}

class N {
    AB ab = new AB();
}








