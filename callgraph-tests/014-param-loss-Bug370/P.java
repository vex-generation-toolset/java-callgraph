class Z {
    public int s;
    public Z() {
    }
}

public class P {
    
    public static int ss;
    public Z z;
    
    public int boo(Z b) {
       return b.s;
    }

}

class Q{
    
    public static void fun(P p) {/*<<<<<20,4,24,4,callee,Z:Z()V,P:boo(QZ;)I,P:P()VSC*/
        Z z = new Z();
        z.s = 10;
        P.ss = p.boo(z);
    }
}

//Issue 1403
//We include the virtual static constructor in call-graph.
//In marker we add suffix VSC (Virtual Static Constructor)
