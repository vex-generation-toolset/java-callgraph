// Issue 1123
// Previously we skipped abstract methods that have no bodu (null body)
// But in 1122, we started keeping them with 'null' set as 'methodHash'
// This lead to a NullPointerException


interface S {
    void foo();
}

public abstract class Z implements S {
    abstract void foo();
}

class B {
    S z;
    
    void bar() {/*<<<<< 18,4,18,14,callee,X:foo()V*/
        z.foo();
    }
}

class X  extends Z {
    void foo() {
        System.out.println("X");
    }
}
