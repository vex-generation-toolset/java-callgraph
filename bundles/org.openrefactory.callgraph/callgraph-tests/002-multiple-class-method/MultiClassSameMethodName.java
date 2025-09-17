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
}

class C extends A {
    
    A foo(A x) {
        
        return this;
    }
}


public class MultiClassSameMethodName {
    public static void main(String[] args) {
        A x = new A();
        int i = 10;
        while(i>0) {
            x = x.foo(new B()); /*<<<<<8,4,10,4,caller,MultiClassSameMethodName:main([QString;)V,D:foo(QA;)QA;*/
            i--; /*<<<<<35,16,35,29,caller, MultiClassSameMethodName:main([QString;)V,D:foo(QA;)QA;*/
        }
        A y = new C();
        y.foo(x);
    }
}

// Explanation:
// (1) marker 1 selected class B's foo() method and its caller is according
//     to the algorithm class D's foo() and class MultiClassSameMethodName's main().
// (2) marker 2 selected x.foo(new B()) and its caller is class D's foo()
//     and MultiClassWithOverloadedMethods's main()
//
// Caller/Callee method signature meaning:
//     class_name : method_name (method_parameter_types...) return_type
//  
//     class types have Q as prefix and ; as suffix
//     primitives have no prefix or suffix
//     array types have [ as prefix