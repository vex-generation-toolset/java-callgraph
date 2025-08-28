// Issue 411 & 547
// Instance variable init through static method call

public class Z {
    String cc = B.getMsg();


    
}

class A{
    Z z;
    String str;
    void foo() {
        B.getMsg();
        z = new Z();/*<<<<<16,12,16,18,callee,B:getMsg()QString;*/
    }
    
}

class B{
    static String getMsg() {
        return "hello";
    }
 
}












