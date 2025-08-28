// Issue 411 & 547
// Instance variable init through super method call
class M {
    String getM() {
        return "hello";
    }
}
class Y{
    M getMsg() {
        return new M();
    }
    
}
public class Z extends Y {
    String cc = super.getMsg().getM();/*<<<<<25,12,25,18,callee,M:getM()QString;,Y:getMsg()QM;,Y:Y()V*/
    
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












