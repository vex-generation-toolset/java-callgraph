// Issue 3
// Mix of static and non-static initializers.

package org.openrefactory.test;

class Counter {
    // static field shared by all objects
    static String count;
    String m;
    static String m1 = new D().getS();
    String m2;

    static {
       count = new B().getS();
    }
    
    {
        m = new C().getS();
    }

    Counter() {
        m2 = m;
    }

    public static void main(String[] args) {
        Counter a = new Counter();
        // Access static field through class name (preferred)
        System.out.println("Objects created: " + a.count);
    }
}

class B {
    public String getS() {
        return "sb";
    }
}

class C {
    public String getS() {
        return "sc";
    }
}

class D {
    public String getS() {
        return "sd";
    }
}

// Default constructor just points to the non-static initializations

/*$$$$$ org.openrefactory.test.Counter.<init>(), 1, 
  		18,13, 2, org.openrefactory.test.C.<init>(), org.openrefactory.test.C#getS(),
*/


// Static method points to static constructor

/*$$$$$ org.openrefactory.test.Counter.main(String[]), 2,
		26,21, 1, org.openrefactory.test.Counter#Counter(),
		25,5, 1, org.openrefactory.test.Counter.<staticinit>(),
*/


// Static constructor just points to the static initializations
// There is no concept of a static constructor in Java, this is more
// like a defualt constructor for static variable initialization.

/*$$$$$ org.openrefactory.test.Counter.<staticinit>(), 2,
		10,24, 2, org.openrefactory.test.D.<init>(), org.openrefactory.test.D#getS(),
		14,16, 2, org.openrefactory.test.B.<init>(), org.openrefactory.test.B#getS(),
*/


// Regular constructor points to default constructor even though it uses static method inside
// This is because the static value is not processed here as this method is not a root.
// The static values get processed at the root.

/*$$$$$ org.openrefactory.test.Counter#Counter(), 1, 
		21,5, 1, org.openrefactory.test.Counter.<init>(),
*/
