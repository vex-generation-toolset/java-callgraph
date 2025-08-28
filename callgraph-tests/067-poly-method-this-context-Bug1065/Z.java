// Issue 1065
// Polymorphic method invoked from this context
// Previously we were not getting Z:foo as callees of X:bar
// Here we should not get Y:foo ar bar is overridden in Y. So, invoking 
// bar from Y's context would not invoke X:bar

class X {
    void foo() {
        System.out.println("X");
    }

    void bar() {/*<<<<< 12,4,12,14,callee,X:foo()V##Y:foo()V##Z:foo()V*/
        foo();
    }
}

class Y extends X {
    void foo() {
        System.out.println("Y");
    }
    
    void bar() {
        System.out.println("Y:Bar");
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