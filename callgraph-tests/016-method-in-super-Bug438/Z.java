// Issue 411 
// Instance variable init through super method call
class N {
    String getM() {
        return "hello";
    }
}

class M extends N {
    
}

class Y{
    M getMsg() {
        return new M();
    }
    
}
public class Z extends Y {
    String cc = super.getMsg().getM();/*<<<<<30,12,30,18,callee,N:getM()QString;,Y:getMsg()QM;,Y:Y()V*/
    
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












