class Z {
    public int s;
    public Z() {
    }
}

class O {
    public int boo(Z b) {
        
    }
}

class M extends O {
    @Override
    public int boo(Z b) {
        return b.s;
     }
}

class N extends O {
    @Override
    public int boo(Z b) {
        
    }
}

public class P extends M {
    public static int ss;
    public Z z;
}

class Q{
    
    public static void fun(O p) {/*<<<<<34,4,39,4,callee,M:boo(QZ;)I,N:boo(QZ;)I,O:boo(QZ;)I,P:P()VSC,Z:Z()V*/
        Z z = new Z();
        z.s = 10;
        
        P.ss = p.boo(z);
    }
    
    public static void fee() {
        O o = new M();
        fun(o);
    }
}

//Issue 1403
//We include the virtual static constructor in call-graph.
//In marker we add suffix VSC (Virtual Static Constructor)
