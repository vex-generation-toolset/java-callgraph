public class B implements A {
    public Z a;
    public B(Z z) {
        a = z;
    }
    
    public A bar(Z z) {
        return this;
    }
    
    public Z foo() {
        return a;/*<<<<<11,4,13,4,caller,C:bazz()V*/
    }
}