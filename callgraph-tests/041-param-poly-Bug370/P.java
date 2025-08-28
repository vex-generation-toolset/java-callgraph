class Z {
    public int s;
    public Z() {
    }
}

class M {
    public int boo(Z b) {
        return b.s;
     }
}

public class P extends M {
    public static int ss;
    public Z z;
}

class N extends M {
    
}

class Q{
    
    public static void fun(P p) {/*<<<<<24,4,28,4,callee,Z:Z()V,M:boo(QZ;)I,P:P()VSC*/
        Z z = new Z();
        z.s = 10;
        P.ss = p.boo(z);
    }
}

//Issue 1403
//We include the virtual static constructor in call-graph.
//In marker we add suffix VSC (Virtual Static Constructor)
