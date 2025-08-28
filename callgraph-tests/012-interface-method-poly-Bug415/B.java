public class B implements A {
    public Z a;
    public B() {
        a = new Z();
    }
    
    public A bar(Z z) {
        return this;
    }
    
    public Z foo() {
        return a;/*<<<<<11,4,13,4,caller,C:bazz()V*/
    }
}