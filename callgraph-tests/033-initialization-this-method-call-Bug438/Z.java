// Issue 411 & 547
// Instance variable init through this method call

public class Z extends Y {
    String cc = this.getMsg();
    

    
    String getMsg() {
        return "hello";/*<<<<<21,12,21,18,callee,Z:getMsg()QString;*/
    }
    
}

class A{
    Z z;
    String str;
    void foo() {
        Y y;
        y.getMsg();
        z = new Z();
    }
    
}
class B{
    
}












