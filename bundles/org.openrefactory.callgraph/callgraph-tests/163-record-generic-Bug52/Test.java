// Issue 52
// Test for generic record

record Box<T>(T content) {}

public class DemoClass {
    public static void foo() {
        Box<String> b = new Box<>("hello");
        b.content();
    }
}

/*$$$$$ DemoClass.foo(), 2,
  		8,25, 1, javax.swing.Box#Box(String),
  		9,9, 1, javax.swing.Box#content()
*/
