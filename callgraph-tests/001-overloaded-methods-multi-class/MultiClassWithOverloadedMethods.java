import java.util.ArrayList;

class A {
    A foo(A x) {
        return x;
    }
}

class B extends A {
    A foo(A x) {
        return new D();
    }
}

class D extends A {
    A foo(A x) {
        A b = new B();
        b.foo(x);
        return new A();
    }
    
    A foo(A x, ArrayList<A> y) {
        return y;
    }
}

class C extends A {
    
    A foo(A x) {
        D y = new D();
        y.foo(x);
        return this;
    }
}


public class MultiClassWithOverloadedMethods {
    public static void main(String[] args) {
        A x = new A();
        int i = 10;
        while(i>0) {
            x = x.foo(new B()); /*<<<<<16,4,20,4,caller,D:foo(QA;)QA;,C:foo(QA;)QA;,MultiClassWithOverloadedMethods:main([QString;)V*/
            i--; /*<<<<<42,16,42,29,caller, C:foo(QA;)QA;##D:foo(QA;)QA;##MultiClassWithOverloadedMethods:main([QString;)V*/
        }
        A y = new C();
        y.foo(x);
    }
}

// Explanation:
// (1) marker 1 selected class D's foo() method and its caller is according
//     to the algorithm class D's foo() and class C's foo().
// (2) marker 2 selected x.foo(new B()) and its caller is class D's foo()
//     and MultiClassWithOverloadedMethods's main()
//
// Caller/Callee method signature meaning:
//     class_name : method_name (method_parameter_types...) return_type
//     
//     class types have Q as prefix and ; as suffix
//     primitives have no prefix or suffix
//     array types have [ as prefix