// Issue 411 & 547 
// Instance variable init through super method call
class Y{
    String getMsg() {
        return "hello";
    }
    
}
public class Z extends Y {
    String cc = super.getMsg();/*<<<<<21,12,21,18,callee,Y:getMsg()QString;,Y:Y()V*/

    
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












