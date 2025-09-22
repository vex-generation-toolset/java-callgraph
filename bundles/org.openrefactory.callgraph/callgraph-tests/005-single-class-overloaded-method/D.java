
public class D extends A {
    A foo(A x) {
        A b = new B();
        b.foo(x);
        return new A();
    }

    A foo(A x, int i) { /*<<<<<9,4,11,4,caller,C:foo(QA;)QA;*/
        return x;
    }
}

// Explanation:
// (1) marker selected D's foo(A x, int i) method and its caller is class 
//     C's foo()
//
// Caller/Callee method signature meaning:
//     class_name : method_name (method_parameter_types...) return_type
//
//     class types have Q as prefix and ; as suffix
//     primitives have no prefix or suffix
//     array types have [ as prefix