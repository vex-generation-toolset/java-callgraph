// Issue 3
// Extended call graph entry for a super method call in an inheritance hierarchy
// Shows three different variations of call to display() method.

package org.openrefactory.test;

class Outer {
    String display() { return "outer"; }
}

class SubOuter extends Outer {
    @Override
    String display() { return "sub"; }

    class Inner extends BaseInner {
        @Override
        String display() { return "inner"; }

        void show() {
            // Call Inner’s own display
            System.out.println("this.display(): " + this.display());

            // Call BaseInner’s version (super of Inner)
            System.out.println("super.display(): " + super.display());

            // Call Outer’s version (super of SubOuter)
            System.out.println("SubOuter.super.display(): " + SubOuter.super.display());
        }
    }

    public static void main(String[] args) {
        SubOuter.Inner inner = new SubOuter().new Inner();
        inner.show();
    }
}

class BaseInner {
    String display() { return "base inner"; }
}


/*$$$$$ org.openrefactory.test.SubOuter.main(String[]), 
   3,
   org.openrefactory.test.SubOuter.<init>(),
   org.openrefactory.test.SubOuter.Inner.<init>(),
   org.openrefactory.test.SubOuter.Inner#show(),
*/

/*$$$$$ org.openrefactory.test.SubOuter.Inner#show(), 
   3,
   org.openrefactory.test.BaseInner#display(),
   org.openrefactory.test.SubOuter.Inner#display(),
   org.openrefactory.test.Outer#display(),
*/

/*!!!!! org.openrefactory.test.SubOuter.Inner#show(), 3, 548,14, 676,15, 813,24 */
