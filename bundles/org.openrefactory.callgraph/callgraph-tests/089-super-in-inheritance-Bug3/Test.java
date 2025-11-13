// Issue 3
// Extended call graph entry for a super method call in an inheritance hierarchy

package org.openrefactory.test;

class Outer {
    void display() { System.out.println("Outer display"); }
}

class SubOuter extends Outer {
    void display() { System.out.println("SubOuter display"); }

    class Inner {
        void show() {
            SubOuter.super.display(); // calls Outer.display()
        }
    }

    public static void main(String[] args) {
        SubOuter.Inner inner = new SubOuter().new Inner();
        inner.show();
    }
}


/*$$$$$ org.openrefactory.test.SubOuter.main(String[]), 
   3,
   org.openrefactory.test.SubOuter.<init>(),
   org.openrefactory.test.SubOuter.Inner.<init>(),
   org.openrefactory.test.SubOuter.Inner#show(),
 */

// This second test is wrong, we should be pointing to Outer#display()
// But we were pointing to SubOuter#display()
// Reported and fixed in Issue 7

/*$$$$$ org.openrefactory.test.SubOuter.Inner#show(), 
   1,
   org.openrefactory.test.Outer#display(),
*/

/*!!!!! org.openrefactory.test.SubOuter.main(String[]), 3, 494,14, 494,26, 530,12 */
/*!!!!! org.openrefactory.test.SubOuter.Inner#show(), 1, 350,24 */
