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

/*$$$$$ org.openrefactory.test.SubOuter.main(String[]), 2,
   		32,32, 2, org.openrefactory.test.SubOuter.<init>(), org.openrefactory.test.SubOuter.Inner.<init>(),
   		33,9, 1, org.openrefactory.test.SubOuter.Inner#show(),
*/

/*$$$$$ org.openrefactory.test.SubOuter.Inner#show(), 3,
   		21,53, 1, org.openrefactory.test.SubOuter.Inner#display(),
   		24,54, 1, org.openrefactory.test.BaseInner#display(),
   		27,63, 1, org.openrefactory.test.Outer#display(),
*/
