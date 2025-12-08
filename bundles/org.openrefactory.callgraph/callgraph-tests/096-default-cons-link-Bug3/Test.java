// Issue 3
// Extended call graph entry for a constructor
// to a default constructor.

package org.openrefactory.test;

class Counter {
    String count = new B().getS();
    String m;

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

/*$$$$$ org.openrefactory.test.Counter.main(String[]), 1,
   		16,21, 1, org.openrefactory.test.Counter#Counter(),
*/

/*$$$$$ org.openrefactory.test.Counter.<init>(), 1,
   		8,20, 2, org.openrefactory.test.B.<init>(), org.openrefactory.test.B#getS(),
*/

// This is the test that demonstrates this new feature.

/*$$$$$ org.openrefactory.test.Counter#Counter(), 1,
   		11,5, 1, org.openrefactory.test.Counter.<init>(),
*/
