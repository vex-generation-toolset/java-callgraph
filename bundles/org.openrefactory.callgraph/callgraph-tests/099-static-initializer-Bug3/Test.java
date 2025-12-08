// Issue 3
// Extended call graph entry for a static constructor
// to a method call inside a static initializer block.

package org.openrefactory.test;

class Counter {
    // static field shared by all objects
    static String count;
    String m;

    static {
       count = new B().getS();
    }

    Counter() {
        m = count;
    }

    public static void main(String[] args) {
        Counter a = new Counter();
        // Access static field through class name (preferred)
        System.out.println("Objects created: " + a.count);
    }
}

class B{
    public String getS() {
        return "ss";
    }
}

/*$$$$$ org.openrefactory.test.Counter.<staticinit>(), 1,
  		13,16, 2, org.openrefactory.test.B.<init>(), org.openrefactory.test.B#getS(),
*/

