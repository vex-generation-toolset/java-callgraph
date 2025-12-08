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

/*$$$$$ org.openrefactory.test.SubOuter.main(String[]), 2,
   		20,32, 2, org.openrefactory.test.SubOuter.<init>(), org.openrefactory.test.SubOuter.Inner.<init>(),
   		21,9, 1, org.openrefactory.test.SubOuter.Inner#show(),
*/

/*$$$$$ org.openrefactory.test.SubOuter.Inner#show(), 1,
   		15,13, 1, org.openrefactory.test.Outer#display(),
*/
