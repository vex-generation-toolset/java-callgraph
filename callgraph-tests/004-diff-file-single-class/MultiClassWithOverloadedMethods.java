public class MultiClassWithOverloadedMethods {
    public static void main(String[] args) {
        A x = new A();
        int i = 10;
        while(i>0) {
            x = x.foo(new B());
            i--; /*<<<<<6,16,6,29,caller, D:foo(QA;)QA;,MultiClassWithOverloadedMethods:main([QString;)V,MyClass:bar()I*/
        }
        A y = new C();
        y.foo(x);
        MyClass mc = new MyClass();
        mc.bar();
    }
}

// Explanation:
// (1) marker selected x.foo(new B()) and its caller is class D's foo()
//     and MultiClassWithOverloadedMethods's main()
//
// Caller/Callee method signature meaning:
//     class_name : method_name (method_parameter_types...) return_type
//  
//     class types have Q as prefix and ; as suffix
//     primitives have no prefix or suffix
//     array types have [ as prefix