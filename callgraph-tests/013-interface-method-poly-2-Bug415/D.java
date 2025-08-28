public class D implements A {
    public Z a;
    public D() {
        a = new Z();
    }
    
    public A bar(Z z) {
        return new B(z);
    }
    
    public Z foo() {
        return a;
    }
}