// Issue 411 & 547 
// Instance variable init through method invocation

public class Z {
    B b = new B();
    String cc = b.getA().getMsg();/*<<<<<14,13,14,19,callee,B:B()V,B:getA()QA;,A:getMsg()QString;*/


}

class A{
    Z z;
    void foo() {
        z = new Z();
    }
    
    String getMsg() {
        return "hello";
    }

}

class B{
    A getA() {
        return new A();
    }
}








