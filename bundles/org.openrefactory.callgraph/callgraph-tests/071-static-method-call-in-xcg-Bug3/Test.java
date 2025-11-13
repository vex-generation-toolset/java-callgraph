// Issue 3
// Extended call graph entry for static method invocations 

public class Test {
    public static void main(String[] args) {
        foo();
        Test.bar();
    }
    
    public static void foo() {
        bar();
        String m = A.b();
    }
    
    public static void bar() {
        
    }
}

class A {
    public static String b() {
        return "ss";
    }
}

/*$$$$$ Test.main(String[]), 2, Test.foo(), Test.bar() */

/*!!!!! Test.main(String[]), 2, 145,5, 160, 10 */

/*$$$$$ Test.foo(), 2, A.b(), Test.bar() */
/*$$$$$ Test.bar(), 0 */

/*$$$$$ A.b(), 0 */
