// Issue 411 
// Instance variable init through non static method call

public class Z {
    String cc = getMsg();
    
    String getMsg() {
        return "hello";/*<<<<<18,12,18,18,callee,Z:getMsg()QString;*/
    }

}

class A{
    Z z;
    String str;
    void foo() {
        z.getMsg();
        z = new Z();
    }
    
}
//Issue 547











