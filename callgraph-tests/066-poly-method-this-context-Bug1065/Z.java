// Issue 1065
// Polymorphic method invoked from this context
// Previously we were not getting Y:foo and Z:foo as callees of X:bar

class X {
    void foo() {
        System.out.println("X");
    }

    void bar() {/*<<<<< 10,4,10,14,callee,X:foo()V,Y:foo()V,Z:foo()V*/
        foo();
    }
}

class Y extends X {
    void foo() {
        System.out.println("Y");
    }
}

class Z extends X {
    void foo() {
        System.out.println("Z");
    }
}

public class Main() {
    public static void main(String[] args) {
        X x = new Y();
        x.bar();
    }
}