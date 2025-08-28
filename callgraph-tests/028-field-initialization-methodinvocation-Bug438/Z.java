// Issue 411 
// Instance variable init through method invocation

public class Z {
    B b = new B();
    String cc = b.getMsg();

}

class A{
    Z z;
    void foo() {
        z = new Z();/*<<<<<13,12,13,18,callee,B:B()V,B:getMsg()QString;*/
    }

}

class B{
    String getMsg() {
        return "hello";
    }
}

//Issue 547







