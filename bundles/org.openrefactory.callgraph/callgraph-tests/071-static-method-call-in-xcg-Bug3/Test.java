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

/*$$$$$ Test.main(String[]), 2,
		6,9, 1, Test.foo(),
		7,9, 1, Test.bar()
*/

/*$$$$$ Test.foo(), 2,
		12,20, 1, A.b(),
		11,9, 1, Test.bar()
*/

/*$$$$$ Test.bar(), 0 */

/*$$$$$ A.b(), 0 */
