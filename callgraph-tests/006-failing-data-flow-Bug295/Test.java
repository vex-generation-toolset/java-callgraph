package Test.java;
// Issue 295
// Method local inner class
class A {
    String str;
    public void bazz() {
        str = "aa";
        System.out.println(str);
    }
}
/* (*this).str, null, *"aa" */
class B extends A {
    public void bazz() {
        str = "ab";
        System.out.println(str);
    }
}
/* (*this).str, null, *"ab" */
class C {
    public A a;
    public C() {
       a = new A();
    }
    public A getA() {
       return a;
    }
}
class D extends C {
    public B a;
    public D() {
       a = new B();
    }
    public A getA() {
       return a;
    }
}
public class Test {
    public void bar(C m) {/*<<<<<38,4,40,4,callee,A:bazz()V,B:bazz()V,C:getA()QA;,D:getA()QA;*/
        m.getA().bazz();
    }
    public static void main(String[] args) {
        Test test = new Test();
        test.bar(new C());
        test.bar(new D());
    }
}
