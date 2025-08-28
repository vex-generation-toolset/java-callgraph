
class A {    
    public A() {
        
    }
    
    public void bar() {
        
    }
}

public class Z {
    A[] arr = new A[] {bazz()};
    public Z() {
    }
    
    A bazz() {
        return new A();
    }
    
    void foo() {
        arr[0].bar();/*<<<<<7,4,9,4,caller,Z:foo()V*/
    }
}
