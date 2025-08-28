
class A {    
    public A() {
        
    }
    
    public void bar() {
        
    }
}

class B {
    public B() {
        
    }
}

class C {
    public C() {
        
    }
}

public class Z {
    String z;
    public Z() {
    }
    
    static void foo() {
        A a = new A();
        if(isWater(a)) {
            z = "haha";
        }/*<<<<<36,4,38,4,caller,Z:foo()V*/
    }
    
    static boolean isWater(A a) {
        return true;
    }
    
    static boolean isWater(B b, C c) {
        return true;
    }
}
