// Issue 3
// Extended call graph entry for a default constructor
// to a method call inside a normal initializer block.

// The initializer block is not distinguished.
// Instead the method call inside an initializer is treated like just another
// field initialization inside the class field area.

package org.openrefactory.test;

class Counter {
    String count;
    String m;

    {
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

/*$$$$$
  org.openrefactory.test.Counter.<init>(), 2, 
  org.openrefactory.test.B.<init>(),
  org.openrefactory.test.B#getS(),
*/
