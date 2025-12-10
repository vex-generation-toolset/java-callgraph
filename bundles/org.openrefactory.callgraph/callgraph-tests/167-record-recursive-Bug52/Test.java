// Issue 52
// Test for recursive record

record Node(int val, Node next) {}

public class Demo {
    public static void foo() {
        Node n = new Node(1, null);
        n.next();
    }
}

/*$$$$$ Demo.foo(),
  2,
  Node#Node(int@@@null),
  Node#next()
*/
