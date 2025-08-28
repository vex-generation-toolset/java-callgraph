// Issue 411 & 547
// Instance variable init through method invocation

public class Z {
    B b = new C();
    String cc = b.getMsg();


}

class A{
    Z z;
    void foo() {
        z = new Z();/*<<<<<14,12,14,18,callee,B:getMsg()QString;##C:C()V##C:getMsg()QString;##D:getMsg()QString;*/
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








